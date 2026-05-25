package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "daily_challenges")
public class DailyChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "challenge_date", nullable = false)
    private LocalDate challengeDate;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions = 10;

    @Column(name = "correct_count", nullable = false)
    private Integer correctCount = 0;

    @Column(name = "prestige_earned", nullable = false)
    private Integer prestigeEarned = 0;

    @Column(name = "combo_count", nullable = false)
    private Integer comboCount = 0;

    @Column(nullable = false)
    private Boolean completed = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
