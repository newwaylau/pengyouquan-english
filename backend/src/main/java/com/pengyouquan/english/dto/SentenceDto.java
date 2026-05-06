package com.pengyouquan.english.dto;

import com.pengyouquan.english.model.Sentence;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 句子 DTO
 */
@Data
@AllArgsConstructor
public class SentenceDto {
    private Long id;
    private String text;
    private Long showId;
    private String showName;
    private String episodeInfo;
    private String audioFile;
    private Double startTime;
    private Double endTime;
    private Boolean isDisabled;

    public static SentenceDto from(Sentence s, String showName) {
        return new SentenceDto(
            s.getId(), s.getText(), s.getShowId(), showName,
            s.getEpisodeInfo(), s.getAudioFile(),
            s.getStartTime(), s.getEndTime(),
            s.getIsDisabled() != null && s.getIsDisabled()
        );
    }
}
