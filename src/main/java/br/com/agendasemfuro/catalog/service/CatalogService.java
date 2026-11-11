package br.com.agendasemfuro.catalog.service;

import br.com.agendasemfuro.catalog.entity.ServiceOffering;
import br.com.agendasemfuro.catalog.repository.ServiceOfferingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CatalogService {
    private final ServiceOfferingRepository repository;

    public CatalogService(ServiceOfferingRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ServiceOffering> listActive() {
        return repository.findAllByActiveTrueOrderByNameAsc();
    }

    @Transactional
    public ServiceOffering create(String name, int durationMinutes, BigDecimal price) {
        return repository.save(new ServiceOffering(name.trim(), durationMinutes, price));
    }
}
