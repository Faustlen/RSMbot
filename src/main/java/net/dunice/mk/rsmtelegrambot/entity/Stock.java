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
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "stocks")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer stocksId;

    @ManyToOne
    @JoinColumn(name = "partner_tg_id", nullable = false)
    private Partner partnerTelegramId;

    @Column(nullable = true)
    private byte[] image;

    @Column(nullable = false)
    private String head;

    @Column(nullable = false, name = "period_stocks_start")
    private LocalDate periodStocksStart;

    @Column(nullable = false, name = "period_stocks_end")
    private LocalDate periodStocksEnd;

    @Column(nullable = false)
    private String description;
}
