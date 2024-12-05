package net.dunice.mk.rsmtelegrambot.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "checks")
public class Check {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "partner_id", nullable = false)
    private Partner partnerId;

    @Column(name = "check_sum", nullable = false)
    private Double checkSum;

    @Column(name = "discount_percent", nullable = false)
    private Integer discountPercent;

    @ManyToOne
    @JoinColumn(name = "user_card", nullable = false)
    private User userCard;

    @Column(nullable = false)
    private LocalDate date;
}

