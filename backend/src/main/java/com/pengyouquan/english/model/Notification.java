package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统通知实体
 * 对应 notifications 表
 */
@Data
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 通知标题 */
    @Column(nullable = false, length = 200)
    private String title;

    /** 通知内容 */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** 是否已发布（true=已发布，公开可见） */
    @Column(nullable = false)
    private Boolean published = false;

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
