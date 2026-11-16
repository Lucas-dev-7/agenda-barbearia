package br.com.agendasemfuro.schedule.service;

import br.com.agendasemfuro.barber.entity.Barber;
import br.com.agendasemfuro.barber.repository.BarberRepository;
import br.com.agendasemfuro.catalog.entity.ServiceOffering;
import br.com.agendasemfuro.catalog.repository.ServiceOfferingRepository;
import br.com.agendasemfuro.schedule.dto.AvailabilityResponse;
import br.com.agendasemfuro.schedule.dto.CreateWorkingHoursRequest;
import br.com.agendasemfuro.schedule.entity.WorkingHours;
import br.com.agendasemfuro.schedule.repository.WorkingHoursRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ScheduleService {
    private static final int SLOT_INTERVAL_MINUTES = 15;

    private final BarberRepository barberRepository;
    private final ServiceOfferingRepository serviceOfferingRepository;
    private final WorkingHoursRepository workingHoursRepository;

    public ScheduleService(
            BarberRepository barberRepository,
            ServiceOfferingRepository serviceOfferingRepository,
            WorkingHoursRepository workingHoursRepository
    ) {
        this.barberRepository = barberRepository;
        this.serviceOfferingRepository = serviceOfferingRepository;
        this.workingHoursRepository = workingHoursRepository;
    }

    @Transactional(readOnly = true)
    public List<WorkingHours> listWorkingHours(UUID barberId) {
        requireBarber(barberId);
        return workingHoursRepository.findAllByBarberIdOrderByDayOfWeekAscOpensAtAsc(barberId);
    }

    @Transactional
    public WorkingHours addWorkingHours(UUID barberId, CreateWorkingHoursRequest request) {
        requireBarber(barberId);

        if (!request.closesAt().isAfter(request.opensAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "closesAt must be after opensAt");
        }

        boolean overlaps = workingHoursRepository
                .existsByBarberIdAndDayOfWeekAndOpensAtLessThanAndClosesAtGreaterThan(
                        barberId,
                        request.dayOfWeek(),
                        request.closesAt(),
                        request.opensAt()
                );
        if (overlaps) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Working hours overlap an existing interval");
        }

        return workingHoursRepository.save(new WorkingHours(
                barberId,
                request.dayOfWeek(),
                request.opensAt(),
                request.closesAt()
        ));
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailability(UUID barberId, UUID serviceId, LocalDate date) {
        requireBarber(barberId);
        ServiceOffering service = serviceOfferingRepository.findById(serviceId)
                .filter(ServiceOffering::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active service not found"));

        List<LocalTime> slots = new ArrayList<>();
        List<WorkingHours> intervals = workingHoursRepository.findAllByBarberIdAndDayOfWeekOrderByOpensAtAsc(
                barberId,
                (short) date.getDayOfWeek().getValue()
        );

        for (WorkingHours interval : intervals) {
            LocalTime lastStart = interval.getClosesAt().minusMinutes(service.getDurationMinutes());
            for (LocalTime start = interval.getOpensAt(); !start.isAfter(lastStart); start = start.plusMinutes(SLOT_INTERVAL_MINUTES)) {
                slots.add(start);
            }
        }

        return new AvailabilityResponse(barberId, serviceId, date, service.getDurationMinutes(), List.copyOf(slots));
    }

    private Barber requireBarber(UUID barberId) {
        return barberRepository.findById(barberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Barber not found"));
    }
}
