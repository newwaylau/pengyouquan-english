package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "guild_treasures")
public class GuildTreasure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "guild_id", nullable = false)
    private Long guildId;

    @Column(nullable = false)
    private Integer milestone;

    @Column(name = "chest_type", nullable = false, length = 20)
    private String chestType;

    @Column(name = "claimed_count", nullable = false)
    private Integer claimedCount = 0;

    @Column(name = "max_claims", nullable = false)
    private Integer maxClaims = 30;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
