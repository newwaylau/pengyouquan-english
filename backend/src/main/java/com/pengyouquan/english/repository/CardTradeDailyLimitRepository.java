package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.CardTradeDailyLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface CardTradeDailyLimitRepository extends JpaRepository<CardTradeDailyLimit, Long> {
    Optional<CardTradeDailyLimit> findByUserIdAndTradeDate(Long userId, LocalDate tradeDate);
}
