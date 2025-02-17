package net.dunice.mk.rsmtelegrambot.repository;

import jakarta.transaction.Transactional;
import net.dunice.mk.rsmtelegrambot.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Integer> {

    Optional<Event> findByTitle(String title);

    @Transactional
    @Modifying
    void deleteEventsByEventDateBefore(LocalDateTime eventDate);

    List<Event> findAllByEventDateAfterOrderByEventDateAsc(LocalDateTime now);
}
