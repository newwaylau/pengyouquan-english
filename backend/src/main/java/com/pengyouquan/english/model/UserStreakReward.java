package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_streak_rewards")
public class UserStreakReward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "reward_id", nullable = false)
    private Long rewardId;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @PrePersist
    protected void onCreate() {
        claimedAt = LocalDateTime.now();
    }
}
