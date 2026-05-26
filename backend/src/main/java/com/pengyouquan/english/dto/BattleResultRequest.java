package com.pengyouquan.english.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BattleResultRequest {
    private Long deckId;
    private int score;
    private BigDecimal accuracy;
    private BigDecimal avgDifficulty;
}
