package br.com.agendasemfuro.appointment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateAppointmentRequest(
        @NotNull UUID barberId,
        @NotNull UUID serviceId,
        @NotBlank @Size(max = 120) String customerName,
        @NotBlank @Size(max = 30) String customerPhone,
        @NotNull LocalDateTime startsAt
) {
}
