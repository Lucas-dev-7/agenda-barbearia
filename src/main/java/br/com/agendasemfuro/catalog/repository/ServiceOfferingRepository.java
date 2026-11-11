package br.com.agendasemfuro.catalog.repository;

import br.com.agendasemfuro.catalog.entity.ServiceOffering;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, UUID> {
    List<ServiceOffering> findAllByActiveTrueOrderByNameAsc();
}
