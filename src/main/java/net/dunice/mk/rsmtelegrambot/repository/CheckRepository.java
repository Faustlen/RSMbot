package net.dunice.mk.rsmtelegrambot.repository;

import net.dunice.mk.rsmtelegrambot.entity.Check;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface CheckRepository extends JpaRepository<Check, Integer> {

    @Query("""
        SELECT
            AVG(c.checkSum) AS avgCheck,
            SUM(c.checkSum) AS totalSum,
            SUM(c.checkSum * (c.discountPercent / 100.0)) AS totalDiscount,
            COUNT(c.checkId) AS checkCount
        FROM Check c
        WHERE c.partnerTelegramId.id = :partnerId
            AND c.date >= :startDate
            AND c.date <= :endDate
        """)
    AnalyticsResult getAnalytics(Long partnerId, LocalDateTime startDate, LocalDateTime endDate);

    interface AnalyticsResult {
        Double getAvgCheck();

        Double getTotalSum();

        Double getTotalDiscount();

        Long getCheckCount();
    }
}
