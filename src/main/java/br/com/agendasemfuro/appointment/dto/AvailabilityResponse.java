package br.com.agendasemfuro.appointment.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record AvailabilityResponse(
        UUID barberId,
        UUID serviceId,
        LocalDate date,
        int durationMinutes,
        List<LocalTime> availableSlots
) {
}
