package net.dunice.mk.rsmtelegrambot.service;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.entity.UserEntity;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public Optional<UserEntity> findByTelegramId(Long telegramId) {
        return userRepository.findByTelegramId(telegramId);
    }

    public UserEntity save(UserEntity user) {
        return userRepository.save(user);
    }

}
