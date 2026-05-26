package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "guilds")
public class Guild {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "leader_id", nullable = false)
    private Long leaderId;

    @Column(length = 200)
    private String description = "";

    @Column(name = "member_count", nullable = false)
    private Integer memberCount = 1;

    @Column(name = "max_members", nullable = false)
    private Integer maxMembers = 30;

    @Column(name = "total_cards_collected", nullable = false)
    private Integer totalCardsCollected = 0;

    @Column(name = "weekly_score", nullable = false)
    private Integer weeklyScore = 0;

    @Column(name = "rank_points", nullable = false)
    private Integer rankPoints = 0;

    @Column(name = "league_score", nullable = false)
    private Integer leagueScore = 0;

    @Column(name = "league_rank")
    private Integer leagueRank = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
