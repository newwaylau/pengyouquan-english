package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 练习记录实体
 * 对应 practice_logs 表
 */
@Data
@Entity
@Table(name = "practice_logs")
public class PracticeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "sentence_id", nullable = false)
    private Long sentenceId;

    @Column(nullable = false)
    private Boolean correct = false;

    @Column(name = "correct_count")
    private Integer correctCount = 0;

    @Column(name = "total_words")
    private Integer totalWords = 0;

    private String mode = "sentry";

    @Column(name = "practiced_at")
    private LocalDateTime practicedAt;

    @PrePersist
    protected void onCreate() {
        if (practicedAt == null) practicedAt = LocalDateTime.now();
    }
}
