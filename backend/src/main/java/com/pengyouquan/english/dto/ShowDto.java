package com.pengyouquan.english.dto;

import com.pengyouquan.english.model.Show;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 剧集简要信息 DTO
 */
@Data
@AllArgsConstructor
public class ShowDto {
    private Long id;
    private String name;
    private long sentenceCount;
    private String importedAt;

    public static ShowDto from(Show show, long sentenceCount) {
        return new ShowDto(
            show.getId(), show.getName(), sentenceCount,
            show.getImportedAt() != null ? show.getImportedAt().toString() : ""
        );
    }
}
