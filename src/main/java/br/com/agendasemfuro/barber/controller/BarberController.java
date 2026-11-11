package br.com.agendasemfuro.barber.controller;

import br.com.agendasemfuro.barber.dto.BarberResponse;
import br.com.agendasemfuro.barber.dto.CreateBarberRequest;
import br.com.agendasemfuro.barber.service.BarberService;
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
@RequestMapping("/api/barbers")
public class BarberController {
    private final BarberService service;

    public BarberController(BarberService service) {
        this.service = service;
    }

    @GetMapping
    public List<BarberResponse> list() {
        return service.listActive().stream().map(BarberResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BarberResponse create(@Valid @RequestBody CreateBarberRequest request) {
        return BarberResponse.from(service.create(request.name()));
    }
}
