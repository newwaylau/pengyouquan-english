package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_collections", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "card_id"})
})
public class UserCollection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "card_id", nullable = false)
    private Long cardId;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "is_upgraded", nullable = false)
    private Boolean isUpgraded = false;

    @Column(name = "acquired_from", nullable = false, length = 20)
    private String acquiredFrom = "basic";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
