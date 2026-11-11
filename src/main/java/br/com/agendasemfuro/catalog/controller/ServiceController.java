package br.com.agendasemfuro.catalog.controller;

import br.com.agendasemfuro.catalog.dto.CreateServiceRequest;
import br.com.agendasemfuro.catalog.dto.ServiceResponse;
import br.com.agendasemfuro.catalog.service.CatalogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/services")
public class ServiceController {
    private final CatalogService service;

    public ServiceController(CatalogService service) {
        this.service = service;
    }

    @GetMapping
    public List<ServiceResponse> list() {
        return service.listActive().stream().map(ServiceResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceResponse create(@Valid @RequestBody CreateServiceRequest request) {
        return ServiceResponse.from(service.create(request.name(), request.durationMinutes(), request.price()));
    }
}
