package net.dunice.mk.rsmtelegrambot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dunice.mk.rsmtelegrambot.entity.User;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public boolean isUserRegistered(long telegramId) {
        return userRepository.existsById(telegramId);
    }

    public User getUserByTelegramId(long telegramId) {
        return userRepository.findById(telegramId).orElse(null);
    }

    public void saveUser(User user) {
        userRepository.save(user);
    }
}
