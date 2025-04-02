package net.dunice.mk.rsmtelegrambot.repository;

import jakarta.transaction.Transactional;
import net.dunice.mk.rsmtelegrambot.entity.Role;
import net.dunice.mk.rsmtelegrambot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByTelegramId(Long telegramId);

    List<User> findAllByUserRole(Role role);

    Optional<User> findFirstByUserRole(Role role);

    Optional<User> findByUserCard(Integer userCard);


    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.info = :info WHERE u.telegramId = :telegramId")
    void updateInfoById(@Param("telegramId") Long telegramId, @Param("info") String info);
}
