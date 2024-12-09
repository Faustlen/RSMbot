package net.dunice.mk.rsmtelegrambot.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "tg_id")
    private Long tgId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "user_role")
    private Role userRole;

    @Column(nullable = false, name = "birth_date")
    private LocalDate birthDate;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String info;

    @Column(name = "user_card", nullable = false, unique = true)
    private Integer userCard;
}
