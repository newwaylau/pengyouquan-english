package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "guild_wars")
public class GuildWar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "guild_id", nullable = false)
    private Long guildId;

    @Column(name = "opponent_guild_id", nullable = false)
    private Long opponentGuildId;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column(nullable = false, length = 20)
    private String phase = "preparation";

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "guild_trophies", nullable = false)
    private Integer guildTrophies = 0;

    @Column(name = "opponent_trophies", nullable = false)
    private Integer opponentTrophies = 0;

    @Column(name = "guild_wins", nullable = false)
    private Integer guildWins = 0;

    @Column(name = "opponent_wins", nullable = false)
    private Integer opponentWins = 0;

    @Column(name = "winner_id")
    private Long winnerId;

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
