package net.dunice.mk.rsmtelegrambot.repository;

import net.dunice.mk.rsmtelegrambot.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    Optional<Event> findByTitle(String title);
}
