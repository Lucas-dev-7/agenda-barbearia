package br.com.agendasemfuro.appointment.service;

import br.com.agendasemfuro.appointment.dto.CreateAppointmentRequest;
import br.com.agendasemfuro.appointment.entity.Appointment;
import br.com.agendasemfuro.appointment.entity.AppointmentStatus;
import br.com.agendasemfuro.appointment.repository.AppointmentRepository;
import br.com.agendasemfuro.barber.entity.Barber;
import br.com.agendasemfuro.barber.repository.BarberRepository;
import br.com.agendasemfuro.catalog.entity.ServiceOffering;
import br.com.agendasemfuro.catalog.repository.ServiceOfferingRepository;
import br.com.agendasemfuro.notification.AppointmentCreatedEvent;
import br.com.agendasemfuro.schedule.entity.WorkingHours;
import br.com.agendasemfuro.schedule.repository.WorkingHoursRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class AppointmentService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final int SLOT_INTERVAL_MINUTES = 15;

    private final BarberRepository barberRepository;
    private final ServiceOfferingRepository serviceOfferingRepository;
    private final WorkingHoursRepository workingHoursRepository;
    private final AppointmentRepository appointmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AppointmentService(
            BarberRepository barberRepository,
            ServiceOfferingRepository serviceOfferingRepository,
            WorkingHoursRepository workingHoursRepository,
            AppointmentRepository appointmentRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.barberRepository = barberRepository;
        this.serviceOfferingRepository = serviceOfferingRepository;
        this.workingHoursRepository = workingHoursRepository;
        this.appointmentRepository = appointmentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Appointment create(CreateAppointmentRequest request) {
        Barber barber = barberRepository.findByIdForUpdate(request.barberId())
                .filter(Barber::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active barber not found"));
        ServiceOffering service = serviceOfferingRepository.findById(request.serviceId())
                .filter(ServiceOffering::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active service not found"));

        LocalDateTime startsAt = request.startsAt();
        if (!startsAt.isAfter(LocalDateTime.now(BUSINESS_ZONE))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startsAt must be in the future");
        }
        if (startsAt.getSecond() != 0 || startsAt.getNano() != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startsAt must be on a 15-minute boundary");
        }

        LocalDateTime endsAt = startsAt.plusMinutes(service.getDurationMinutes());
        if (!endsAt.toLocalDate().equals(startsAt.toLocalDate())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Appointments cannot cross midnight");
        }
        List<WorkingHours> intervals = workingHoursRepository.findAllByBarberIdAndDayOfWeekOrderByOpensAtAsc(
                barber.getId(), (short) startsAt.getDayOfWeek().getValue()
        );
        boolean fitsWorkingHours = intervals.stream().anyMatch(interval ->
                isValidSlot(interval, startsAt.toLocalTime(), endsAt.toLocalTime())
        );
        if (!fitsWorkingHours) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Appointment is outside the barber's working hours");
        }

        boolean overlaps = appointmentRepository.existsByBarberIdAndStatusAndStartsAtLessThanAndEndsAtGreaterThan(
                barber.getId(), AppointmentStatus.SCHEDULED, endsAt, startsAt
        );
        if (overlaps) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Appointment overlaps an existing booking");
        }

        Appointment appointment = appointmentRepository.save(new Appointment(
                barber.getId(), service.getId(), request.customerName().trim(), request.customerPhone().trim(),
                startsAt, endsAt
        ));
        eventPublisher.publishEvent(new AppointmentCreatedEvent(
                appointment.getId(), appointment.getCustomerName(), appointment.getCustomerPhone(),
                barber.getName(), service.getName(), appointment.getStartsAt(), appointment.getEndsAt()
        ));
        return appointment;
    }

    @Transactional(readOnly = true)
    public Appointment get(UUID appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
    }

    @Transactional
    public Appointment cancel(UUID appointmentId) {
        Appointment appointment = appointmentRepository.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only scheduled appointments can be cancelled");
        }
        appointment.cancel();
        return appointment;
    }

    private boolean isValidSlot(WorkingHours interval, LocalTime startsAt, LocalTime endsAt) {
        if (startsAt.isBefore(interval.getOpensAt()) || endsAt.isAfter(interval.getClosesAt())) {
            return false;
        }
        long minutesFromOpening = Duration.between(interval.getOpensAt(), startsAt).toMinutes();
        return minutesFromOpening % SLOT_INTERVAL_MINUTES == 0;
    }
}
