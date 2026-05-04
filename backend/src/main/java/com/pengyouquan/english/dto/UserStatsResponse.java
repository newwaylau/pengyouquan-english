package com.pengyouquan.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 用户统计信息响应
 */
@Data
@AllArgsConstructor
public class UserStatsResponse {
    private long totalPractices;
    private long todayPractices;
    private long totalCorrect;
    private long wrongCount;
    private double accuracy;
}
