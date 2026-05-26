package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "card_trade_daily_limits", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "trade_date"})
})
public class CardTradeDailyLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "trade_count", nullable = false)
    private Integer tradeCount = 0;
}
