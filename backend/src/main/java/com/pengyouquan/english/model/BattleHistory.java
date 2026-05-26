package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "battle_history")
public class BattleHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "challenger_id", nullable = false)
    private Long challengerId;

    @Column(name = "defender_id", nullable = false)
    private Long defenderId;

    @Column(name = "winner_id")
    private Long winnerId;

    @Column(name = "challenger_deck_id")
    private Long challengerDeckId;

    @Column(name = "defender_deck_id")
    private Long defenderDeckId;

    @Column(name = "challenger_score")
    private Integer challengerScore = 0;

    @Column(name = "defender_score")
    private Integer defenderScore = 0;

    @Column(name = "challenger_accuracy", precision = 5, scale = 2)
    private BigDecimal challengerAccuracy = BigDecimal.ZERO;

    @Column(name = "defender_accuracy", precision = 5, scale = 2)
    private BigDecimal defenderAccuracy = BigDecimal.ZERO;

    @Column(name = "challenger_avg_difficulty", precision = 4, scale = 2)
    private BigDecimal challengerAvgDifficulty = BigDecimal.ZERO;

    @Column(name = "defender_avg_difficulty", precision = 4, scale = 2)
    private BigDecimal defenderAvgDifficulty = BigDecimal.ZERO;

    @Column(name = "trophy_change")
    private Integer trophyChange = 0;

    @Column(nullable = false, length = 20)
    private String status = "pending";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
