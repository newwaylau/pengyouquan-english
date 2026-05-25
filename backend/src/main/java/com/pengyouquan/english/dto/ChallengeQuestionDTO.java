package com.pengyouquan.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChallengeQuestionDTO {
    private Long id;
    private Long sentenceId;
    private String englishText;
    private String chineseText;
    private String audioFile;
    private boolean answered;
    private Boolean isCorrect;
    private String userAnswer;
}
