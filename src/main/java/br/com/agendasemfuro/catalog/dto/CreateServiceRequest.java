package br.com.agendasemfuro.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateServiceRequest(
        @NotBlank(message = "Informe o nome do serviço.")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.") String name,
        @Positive(message = "A duração deve ser maior que zero.") int durationMinutes,
        @NotNull(message = "Informe o preço do serviço.")
        @DecimalMin(value = "0.00", message = "O preço não pode ser negativo.") BigDecimal price
) {
}
