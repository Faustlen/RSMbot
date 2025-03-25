package net.dunice.mk.rsmtelegrambot.repository;

import net.dunice.mk.rsmtelegrambot.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Integer> {

    List<Stock> findAllBetweenPeriodStocksStartAndBetweenByPeriodStocksEndOrderByPeriodStocksEndAsc(LocalDate now);
}
