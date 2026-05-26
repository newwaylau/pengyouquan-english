package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "user_stats")
public class UserStats {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private Integer trophies = 0;

    @Column(nullable = false)
    private Integer wins = 0;

    @Column(nullable = false)
    private Integer losses = 0;

    @Column(name = "win_streak", nullable = false)
    private Integer winStreak = 0;

    @Column(name = "best_trophies", nullable = false)
    private Integer bestTrophies = 0;
}
