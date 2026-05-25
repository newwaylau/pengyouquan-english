package com.pengyouquan.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PrestigeResponse {
    private int prestige;
    private int rankTier;
    private String titleCn;
    private String titleEn;
    private int nextRequiredPrestige;
    private int progressPercent;
    private int consecutiveDays;
}
