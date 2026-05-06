package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 句子举报记录实体
 * 对应 sentence_flags 表
 * 用户标记句子"跟原音对不上"时使用
 */
@Data
@Entity
@Table(name = "sentence_flags", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"sentence_id", "user_id"})
})
public class SentenceFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sentence_id", nullable = false)
    private Long sentenceId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
