package br.com.agendasemfuro.barber.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBarberRequest(
        @NotBlank @Size(max = 120) String name
) {
}
