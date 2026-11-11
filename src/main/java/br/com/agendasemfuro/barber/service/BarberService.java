package br.com.agendasemfuro.barber.service;

import br.com.agendasemfuro.barber.entity.Barber;
import br.com.agendasemfuro.barber.repository.BarberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BarberService {
    private final BarberRepository repository;

    public BarberService(BarberRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Barber> listActive() {
        return repository.findAllByActiveTrueOrderByNameAsc();
    }

    @Transactional
    public Barber create(String name) {
        return repository.save(new Barber(name.trim()));
    }
}
