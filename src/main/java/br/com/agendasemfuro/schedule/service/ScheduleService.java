package br.com.agendasemfuro.schedule.service;

import br.com.agendasemfuro.barber.entity.Barber;
import br.com.agendasemfuro.barber.repository.BarberRepository;
import br.com.agendasemfuro.schedule.dto.CreateWorkingHoursRequest;
import br.com.agendasemfuro.schedule.entity.WorkingHours;
import br.com.agendasemfuro.schedule.repository.WorkingHoursRepository;
import br.com.agendasemfuro.shared.exception.ApplicationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
public class ScheduleService {
    private final BarberRepository barberRepository;
    private final WorkingHoursRepository workingHoursRepository;

    public ScheduleService(
            BarberRepository barberRepository,
            WorkingHoursRepository workingHoursRepository
    ) {
        this.barberRepository = barberRepository;
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
            throw ApplicationException.invalidRequest("O horário de fechamento deve ser posterior ao de abertura.");
        }

        boolean overlaps = workingHoursRepository
                .existsByBarberIdAndDayOfWeekAndOpensAtLessThanAndClosesAtGreaterThan(
                        barberId,
                        request.dayOfWeek(),
                        request.closesAt(),
                        request.opensAt()
                );
        if (overlaps) {
            throw ApplicationException.conflict("Esse intervalo se sobrepõe a um expediente já cadastrado.");
        }

        return workingHoursRepository.save(new WorkingHours(
                barberId,
                request.dayOfWeek(),
                request.opensAt(),
                request.closesAt()
        ));
    }

    private Barber requireBarber(UUID barberId) {
        return barberRepository.findById(barberId)
                .orElseThrow(() -> ApplicationException.notFound("Barbeiro não encontrado."));
    }
}
