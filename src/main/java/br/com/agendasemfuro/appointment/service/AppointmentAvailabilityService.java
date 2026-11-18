package br.com.agendasemfuro.appointment.service;

import br.com.agendasemfuro.appointment.dto.AvailabilityResponse;
import br.com.agendasemfuro.appointment.entity.Appointment;
import br.com.agendasemfuro.appointment.entity.AppointmentStatus;
import br.com.agendasemfuro.appointment.repository.AppointmentRepository;
import br.com.agendasemfuro.barber.entity.Barber;
import br.com.agendasemfuro.barber.repository.BarberRepository;
import br.com.agendasemfuro.catalog.entity.ServiceOffering;
import br.com.agendasemfuro.catalog.repository.ServiceOfferingRepository;
import br.com.agendasemfuro.schedule.entity.WorkingHours;
import br.com.agendasemfuro.schedule.repository.WorkingHoursRepository;
import br.com.agendasemfuro.shared.exception.ApplicationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AppointmentAvailabilityService {
    private static final int SLOT_INTERVAL_MINUTES = 15;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

    private final BarberRepository barberRepository;
    private final ServiceOfferingRepository serviceOfferingRepository;
    private final WorkingHoursRepository workingHoursRepository;
    private final AppointmentRepository appointmentRepository;

    public AppointmentAvailabilityService(
            BarberRepository barberRepository,
            ServiceOfferingRepository serviceOfferingRepository,
            WorkingHoursRepository workingHoursRepository,
            AppointmentRepository appointmentRepository
    ) {
        this.barberRepository = barberRepository;
        this.serviceOfferingRepository = serviceOfferingRepository;
        this.workingHoursRepository = workingHoursRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailability(UUID barberId, UUID serviceId, LocalDate date) {
        Barber barber = barberRepository.findById(barberId)
                .filter(Barber::isActive)
                .orElseThrow(() -> ApplicationException.notFound("Active barber not found"));
        ServiceOffering service = serviceOfferingRepository.findById(serviceId)
                .filter(ServiceOffering::isActive)
                .orElseThrow(() -> ApplicationException.notFound("Active service not found"));

        LocalDateTime dayStart = date.atStartOfDay();
        List<Appointment> appointments = appointmentRepository
                .findAllByBarberIdAndStatusAndStartsAtLessThanAndEndsAtGreaterThan(
                        barber.getId(), AppointmentStatus.SCHEDULED, dayStart.plusDays(1), dayStart
                );
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        List<LocalTime> availableSlots = new ArrayList<>();
        List<WorkingHours> intervals = workingHoursRepository.findAllByBarberIdAndDayOfWeekOrderByOpensAtAsc(
                barber.getId(), (short) date.getDayOfWeek().getValue()
        );

        for (WorkingHours interval : intervals) {
            LocalTime lastStart = interval.getClosesAt().minusMinutes(service.getDurationMinutes());
            for (LocalTime start = interval.getOpensAt(); !start.isAfter(lastStart); start = start.plusMinutes(SLOT_INTERVAL_MINUTES)) {
                LocalDateTime slotStart = date.atTime(start);
                LocalDateTime slotEnd = slotStart.plusMinutes(service.getDurationMinutes());
                boolean isInPast = !slotStart.isAfter(now);
                boolean isBooked = appointments.stream().anyMatch(appointment ->
                        appointment.getStartsAt().isBefore(slotEnd) && appointment.getEndsAt().isAfter(slotStart)
                );
                if (!isInPast && !isBooked) {
                    availableSlots.add(start);
                }
            }
        }

        return new AvailabilityResponse(
                barber.getId(), service.getId(), date, service.getDurationMinutes(), List.copyOf(availableSlots)
        );
    }
}
