package com.pengyouquan.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LeaderboardEntry {
    private int rank;
    private Long userId;
    private String nickname;
    private String avatar;
    private int prestige;
    private int rankTier;
    private String titleCn;
    private int correctCount;
}
