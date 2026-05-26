package com.pengyouquan.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GrantPackRequest {
    private Long showId;
    private double accuracy;
}
