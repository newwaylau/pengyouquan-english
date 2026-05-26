package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "streak_rewards")
public class StreakReward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "days_required", nullable = false, unique = true)
    private Integer daysRequired;

    @Column(name = "reward_type", length = 20, nullable = false)
    private String rewardType;

    @Column(name = "reward_name", length = 100, nullable = false)
    private String rewardName;

    @Column(name = "reward_icon", length = 10)
    private String rewardIcon = "🎁";

    @Column(length = 200)
    private String description = "";
}
