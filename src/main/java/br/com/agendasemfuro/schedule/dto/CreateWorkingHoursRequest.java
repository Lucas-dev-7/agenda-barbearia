package br.com.agendasemfuro.schedule.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record CreateWorkingHoursRequest(
        @NotNull(message = "Informe o dia da semana.")
        @Min(value = 1, message = "O dia da semana deve estar entre 1 e 7.")
        @Max(value = 7, message = "O dia da semana deve estar entre 1 e 7.") Short dayOfWeek,
        @NotNull(message = "Informe o horário de abertura.") LocalTime opensAt,
        @NotNull(message = "Informe o horário de fechamento.") LocalTime closesAt
) {
}
