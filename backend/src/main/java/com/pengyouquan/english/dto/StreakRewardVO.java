package com.pengyouquan.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StreakRewardVO {
    private Long id;
    private int daysRequired;
    private String rewardType;
    private String rewardName;
    private String rewardIcon;
    private String description;
    private int consecutiveDays;
    private boolean claimed;
}
