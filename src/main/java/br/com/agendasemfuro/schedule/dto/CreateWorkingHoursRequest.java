package br.com.agendasemfuro.schedule.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record CreateWorkingHoursRequest(
        @NotNull @Min(1) @Max(7) Short dayOfWeek,
        @NotNull LocalTime opensAt,
        @NotNull LocalTime closesAt
) {
}
