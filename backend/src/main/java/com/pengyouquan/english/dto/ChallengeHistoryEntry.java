package com.pengyouquan.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class ChallengeHistoryEntry {
    private LocalDate date;
    private int correctCount;
    private int totalQuestions;
    private int prestigeEarned;
}
