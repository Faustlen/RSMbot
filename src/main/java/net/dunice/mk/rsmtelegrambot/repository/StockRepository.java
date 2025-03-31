package net.dunice.mk.rsmtelegrambot.repository;

import net.dunice.mk.rsmtelegrambot.entity.Partner;
import net.dunice.mk.rsmtelegrambot.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface StockRepository extends JpaRepository<Stock, Integer> {

    @Query("SELECT s FROM Stock s " +
        "WHERE s.periodStocksStart <= CURRENT_DATE " +
        "AND s.periodStocksEnd >= CURRENT_DATE " +
        "ORDER BY s.periodStocksEnd ASC")
    List<Stock> findAllCurrentStocks();

    List<Stock> findByPartnerTelegramId(Partner partnerTelegramId);
}
