package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 字幕句子实体
 * 对应 sentences 表
 */
@Data
@Entity
@Table(name = "sentences")
public class Sentence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "show_id", nullable = false)
    private Long showId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "start_time")
    private Double startTime;

    @Column(name = "end_time")
    private Double endTime;

    @Column(name = "audio_file")
    private String audioFile;

    @Column(name = "episode_info")
    private String episodeInfo;

    private Double accuracy;

    @Column(name = "imported_at", updatable = false)
    private LocalDateTime importedAt;

    @PrePersist
    protected void onCreate() {
        importedAt = LocalDateTime.now();
    }
}
