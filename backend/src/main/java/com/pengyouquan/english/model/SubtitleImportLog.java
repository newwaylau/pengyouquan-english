package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 字幕导入记录实体
 * 对应 subtitle_import_logs 表
 * 跟踪每次字幕文件导入的元信息
 */
@Data
@Entity
@Table(name = "subtitle_import_logs")
public class SubtitleImportLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "show_id", nullable = false)
    private Long showId;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(nullable = false, length = 10)
    private String format; // srt / ass / vtt

    @Column(name = "sentence_count")
    private Integer sentenceCount;

    @Column(name = "duplicate_count")
    private Integer duplicateCount;

    @Column(name = "imported_at", updatable = false)
    private LocalDateTime importedAt;

    @PrePersist
    protected void onCreate() {
        importedAt = LocalDateTime.now();
    }
}
