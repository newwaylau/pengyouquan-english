package com.pengyouquan.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CardPackResult {
    private java.util.List<CardResponse> cards;
    private String packType;
}
