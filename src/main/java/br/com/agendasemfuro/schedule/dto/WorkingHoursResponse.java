package br.com.agendasemfuro.schedule.dto;

import br.com.agendasemfuro.schedule.entity.WorkingHours;

import java.time.LocalTime;
import java.util.UUID;

public record WorkingHoursResponse(
        UUID id,
        UUID barberId,
        int dayOfWeek,
        LocalTime opensAt,
        LocalTime closesAt
) {
    public static WorkingHoursResponse from(WorkingHours workingHours) {
        return new WorkingHoursResponse(
                workingHours.getId(),
                workingHours.getBarberId(),
                workingHours.getDayOfWeek(),
                workingHours.getOpensAt(),
                workingHours.getClosesAt()
        );
    }
}
