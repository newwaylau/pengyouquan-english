package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_cards", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "card_id"})
})
public class UserCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "card_id", nullable = false)
    private Long cardId;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "obtained_at", updatable = false)
    private LocalDateTime obtainedAt;

    @PrePersist
    protected void onCreate() {
        obtainedAt = LocalDateTime.now();
    }
}
