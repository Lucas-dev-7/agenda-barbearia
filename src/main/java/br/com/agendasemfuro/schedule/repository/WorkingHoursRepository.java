package br.com.agendasemfuro.schedule.repository;

import br.com.agendasemfuro.schedule.entity.WorkingHours;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface WorkingHoursRepository extends JpaRepository<WorkingHours, UUID> {
    List<WorkingHours> findAllByBarberIdOrderByDayOfWeekAscOpensAtAsc(UUID barberId);

    List<WorkingHours> findAllByBarberIdAndDayOfWeekOrderByOpensAtAsc(UUID barberId, short dayOfWeek);

    boolean existsByBarberIdAndDayOfWeekAndOpensAtLessThanAndClosesAtGreaterThan(
            UUID barberId,
            short dayOfWeek,
            LocalTime closesAt,
            LocalTime opensAt
    );
}
