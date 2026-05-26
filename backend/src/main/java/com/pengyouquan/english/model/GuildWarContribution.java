package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "guild_war_contributions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"war_id", "user_id"})
})
public class GuildWarContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "war_id", nullable = false)
    private Long warId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "cards_contributed", nullable = false)
    private Integer cardsContributed = 0;

    @Column(name = "battles_fought", nullable = false)
    private Integer battlesFought = 0;

    @Column(name = "battles_won", nullable = false)
    private Integer battlesWon = 0;

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
