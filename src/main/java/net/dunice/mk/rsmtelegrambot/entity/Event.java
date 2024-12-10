package net.dunice.mk.rsmtelegrambot.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Integer eventId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, name = "event_date")
    private LocalDateTime eventDate;

    @Column
    private String text;

    @Column
    private String link;
}
