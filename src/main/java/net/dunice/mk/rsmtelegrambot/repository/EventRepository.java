package net.dunice.mk.rsmtelegrambot.repository;

import net.dunice.mk.rsmtelegrambot.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Integer> {
    Optional<Event> findByTitle(String title);

    List<Event> findAllByEventDateAfterOrderByEventDateAsc(LocalDateTime now);
}
