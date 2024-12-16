package net.dunice.mk.rsmtelegrambot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
    @JoinColumn(name = "partner_tg_id", nullable = false)
    private Partner partnerTelegramId;

    @Column(name = "check_sum")
    private BigDecimal checkSum;

    @Column(name = "discount_percent")
    private Short discountPercent;

    @ManyToOne
    @JoinColumn(name = "tg_id", referencedColumnName = "tg_id", nullable = false)
    private User userTelegramId;

    @Column(nullable = false)
    private LocalDateTime date;
}