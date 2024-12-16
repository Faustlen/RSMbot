package net.dunice.mk.rsmtelegrambot.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "checks")
public class Check {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "check_id")
    private Integer checkId;

    @ManyToOne
    @JoinColumn(name = "partner_id", nullable = false)
    private Partner partnerId;

    @Column(name = "check_sum")
    private BigDecimal checkSum;

    @Column(name = "discount_percent")
    private Short discountPercent;

    @ManyToOne
    @JoinColumn(name = "user_card", referencedColumnName = "user_card", nullable = false)
    private User userCard;

    @Column(nullable = false)
    private LocalDateTime date;
}

