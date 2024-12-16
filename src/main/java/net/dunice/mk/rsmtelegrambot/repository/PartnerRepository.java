package net.dunice.mk.rsmtelegrambot.repository;

import net.dunice.mk.rsmtelegrambot.entity.Partner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PartnerRepository extends JpaRepository<Partner, Long> {
    Optional<Partner> findByName(String name);

    @Query("SELECT p FROM Partner p WHERE p.isValid = true AND p.discountDate > CURRENT_TIMESTAMP")
    List<Partner> findValidPartnersWithPresentDiscount();
}
