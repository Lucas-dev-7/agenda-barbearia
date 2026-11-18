package br.com.agendasemfuro.appointment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateAppointmentRequest(
        @NotNull(message = "Selecione um barbeiro.") UUID barberId,
        @NotNull(message = "Selecione um serviço.") UUID serviceId,
        @NotBlank(message = "Informe o nome do cliente.") @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.") String customerName,
        @NotBlank(message = "Informe o telefone do cliente.")
        @Size(max = 30, message = "O telefone deve ter no máximo 30 caracteres.") String customerPhone,
        @NotNull(message = "Informe a data e o horário do agendamento.") LocalDateTime startsAt
) {
}
