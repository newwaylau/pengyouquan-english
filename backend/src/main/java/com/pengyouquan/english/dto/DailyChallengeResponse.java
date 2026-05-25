package com.pengyouquan.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class DailyChallengeResponse {
    private Long challengeId;
    private List<ChallengeQuestionDTO> questions;
    private int answeredCount;
    private int correctCount;
    private boolean completed;
    private int comboCount;
}
