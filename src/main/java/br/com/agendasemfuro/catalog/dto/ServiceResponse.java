package br.com.agendasemfuro.catalog.dto;

import br.com.agendasemfuro.catalog.entity.ServiceOffering;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ServiceResponse(UUID id, String name, int durationMinutes,
                              BigDecimal price, boolean active, LocalDateTime createdAt) {
    public static ServiceResponse from(ServiceOffering service) {
        return new ServiceResponse(service.getId(), service.getName(), service.getDurationMinutes(),
                service.getPrice(), service.isActive(), service.getCreatedAt());
    }
}
