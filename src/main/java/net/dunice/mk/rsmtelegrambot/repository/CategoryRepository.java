package net.dunice.mk.rsmtelegrambot.repository;

import net.dunice.mk.rsmtelegrambot.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    Optional<Category> findByCategoryName(String categoryName);
}
