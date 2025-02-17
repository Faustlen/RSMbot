package net.dunice.mk.rsmtelegrambot.service;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.repository.EventRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class CronService {

    private final GoogleSheetUpdater googleSheetUpdater;

    private final EventRepository eventRepository;

    @Scheduled(cron = "0 0 6 * * ?") // Каждый день в 6 утра
    public void schedule() {
        googleSheetUpdater.updateSheet();
        eventRepository.deleteEventsByEventDateBefore(LocalDateTime.now());
    }
}
