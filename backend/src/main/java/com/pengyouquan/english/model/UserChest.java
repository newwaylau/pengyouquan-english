package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_chests")
public class UserChest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "chest_type", nullable = false, length = 20)
    private String chestType;

    @Column(nullable = false, length = 20)
    private String status = "locked";

    @Column(name = "unlock_progress", nullable = false)
    private int unlockProgress = 0;

    @Column(name = "unlock_required", nullable = false)
    private int unlockRequired;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
