package net.dunice.mk.rsmtelegrambot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDate;

//TODO: Определить итоговый вид сущности пользователя и исправить в соответствии
@Data
@Entity
public class UserEntity {

    @Id
    @Column(name = "tg_id")
    private Long telegramId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false)
    private String info;

    @Column(name = "user_card", nullable = false, unique = true)
    private String userCard;
}
