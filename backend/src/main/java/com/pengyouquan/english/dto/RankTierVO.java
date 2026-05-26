package com.pengyouquan.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RankTierVO {
    private int tier;
    private String titleCn;
    private String titleEn;
    private int requiredPrestige;
    private boolean current;
}
