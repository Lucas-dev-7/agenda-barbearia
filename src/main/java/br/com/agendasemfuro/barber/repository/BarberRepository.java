package br.com.agendasemfuro.barber.repository;

import br.com.agendasemfuro.barber.entity.Barber;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BarberRepository extends JpaRepository<Barber, UUID> {
    List<Barber> findAllByActiveTrueOrderByNameAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select barber from Barber barber where barber.id = :id")
    Optional<Barber> findByIdForUpdate(@Param("id") UUID id);
}
