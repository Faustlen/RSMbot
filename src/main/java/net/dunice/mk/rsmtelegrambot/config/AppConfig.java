package net.dunice.mk.rsmtelegrambot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class AppConfig {

    @Bean("lastBotMessageIdMap")
    public Map<Long, Integer> getLastBotMessageIDMap() {
        return new ConcurrentHashMap<>();
    }

}
