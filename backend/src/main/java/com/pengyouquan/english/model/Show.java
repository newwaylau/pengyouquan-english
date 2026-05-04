package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 剧集实体
 * 对应 shows 表
 */
@Data
@Entity
@Table(name = "shows")
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "source_file")
    private String sourceFile;

    @Column(name = "imported_at", updatable = false)
    private LocalDateTime importedAt;

    @PrePersist
    protected void onCreate() {
        importedAt = LocalDateTime.now();
    }
}
