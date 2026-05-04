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

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (lastPracticedAt == null) lastPracticedAt = LocalDateTime.now();
    }
}
