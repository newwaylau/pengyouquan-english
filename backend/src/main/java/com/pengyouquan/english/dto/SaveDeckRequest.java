package com.pengyouquan.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class SaveDeckRequest {
    private String name;
    private List<Long> cardIds;
}
