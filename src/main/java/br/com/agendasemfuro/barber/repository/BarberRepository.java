package br.com.agendasemfuro.barber.repository;

import br.com.agendasemfuro.barber.entity.Barber;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BarberRepository extends JpaRepository<Barber, UUID> {
    List<Barber> findAllByActiveTrueOrderByNameAsc();
}
