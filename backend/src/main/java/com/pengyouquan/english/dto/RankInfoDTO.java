package com.pengyouquan.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RankInfoDTO {
    private int trophies;
    private String tierName;
    private String tierIcon;
    private int rank;
    private String seasonRewardType;
    private int seasonRewardCount;
    private int winStreak;
    private int wins;
    private int losses;
}
