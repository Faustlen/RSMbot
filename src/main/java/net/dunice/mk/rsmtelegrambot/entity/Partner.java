package net.dunice.mk.rsmtelegrambot.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "partners")
public class Partner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true, name = "phone_number")
    private String phoneNumber;

    @Column(nullable = false, name = "discount_percent")
    private Integer discountPercent;

    @ManyToOne
    @JoinColumn(nullable = false, name = "category_id")
    private Category categoryId;

    @Column(nullable = false)
    private String logo;

    @Column(nullable = false, name = "partners_info")
    private String partnersInfo;

    @Column(nullable = false, name = "discount_date")
    private LocalDate discountDate;
}
