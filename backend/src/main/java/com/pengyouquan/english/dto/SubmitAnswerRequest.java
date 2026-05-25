package com.pengyouquan.english.dto;

import lombok.Data;

@Data
public class SubmitAnswerRequest {
    private Long questionId;
    private String answer;
}
