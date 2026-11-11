package br.com.agendasemfuro.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateServiceRequest(
        @NotBlank @Size(max = 120) String name,
        @Positive int durationMinutes,
        @NotNull @DecimalMin("0.00") BigDecimal price
) {
}
