package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 错题实体
 * 对应 wrong_sentences 表
 */
@Data
@Entity
@Table(name = "wrong_sentences")
public class WrongSentence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "sentence_id", nullable = false)
    private Long sentenceId;

    @Column(name = "error_count")
    private Integer errorCount = 1;

    @Column(name = "last_practiced_at")
    private LocalDateTime lastPracticedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 是否已掌握（标记而非删除） */
    @Column(name = "is_mastered", columnDefinition = "TINYINT", length = 1)
    private Boolean isMastered = false;

    /** 剧集名（冗余存储，方便筛选和排序） */
    @Column(name = "show_name")
    private String showName = "";

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (lastPracticedAt == null) lastPracticedAt = LocalDateTime.now();
        if (isMastered == null) isMastered = false;
    }
}
