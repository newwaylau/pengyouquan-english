package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "guild_treasure_claims")
public class GuildTreasureClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "treasure_id", nullable = false)
    private Long treasureId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "claimed_at", updatable = false)
    private LocalDateTime claimedAt;

    @PrePersist
    protected void onCreate() {
        claimedAt = LocalDateTime.now();
    }
}
