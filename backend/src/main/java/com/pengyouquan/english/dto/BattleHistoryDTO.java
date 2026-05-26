package com.pengyouquan.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class BattleHistoryDTO {
    private Long id;
    private Long challengerId;
    private String challengerNickname;
    private Long defenderId;
    private String defenderNickname;
    private Long winnerId;
    private Integer challengerScore;
    private Integer defenderScore;
    private BigDecimal challengerAccuracy;
    private BigDecimal defenderAccuracy;
    private Integer trophyChange;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
