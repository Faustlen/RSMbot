package net.dunice.mk.rsmtelegrambot.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscountCodeService {

    private final SecureRandom random = new SecureRandom();

    @Value("${discount.code.lifetime.seconds}")
    private int codeLifetime;

    @Value("${discount.code.length}")
    private int codeLength;

    private int codeBound;

    private int discountCode;

    private Instant expirationTime;

    public int getDiscountCode() {
        if (Instant.now().isAfter(expirationTime)) {
            synchronized (this) {
                if (Instant.now().isAfter(expirationTime)) {
                    generateDiscountCode();
                }
            }
        }
        return discountCode;
    }

    public int getSecondsLeft() {
        return (int) Duration.between(Instant.now(), expirationTime).toSeconds();
    }

    @PostConstruct
    public void init() {
        codeBound = (int) Math.pow(10, codeLength);
        generateDiscountCode();
    }

    private void generateDiscountCode() {
        discountCode = random.nextInt(codeBound);
        expirationTime = Instant.now().plusSeconds(codeLifetime);
    }
}
