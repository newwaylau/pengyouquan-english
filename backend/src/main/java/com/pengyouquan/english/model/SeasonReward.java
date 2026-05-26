package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "season_rewards")
public class SeasonReward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "season_number", nullable = false)
    private Integer seasonNumber;

    @Column(name = "final_rank", length = 30)
    private String finalRank;

    @Column(name = "final_trophies")
    private Integer finalTrophies;

    @Column(name = "reward_claimed", nullable = false)
    private Boolean rewardClaimed = false;

    @Column(name = "chest_type", length = 20)
    private String chestType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
