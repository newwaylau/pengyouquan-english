package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "season_rankings", indexes = {
    @Index(name = "idx_season_score", columnList = "season_number,total_score")
})
public class SeasonRanking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "season_number", nullable = false)
    private Integer seasonNumber;

    @Column(name = "pvp_score", nullable = false)
    private Integer pvpScore = 0;

    @Column(name = "expedition_score", nullable = false)
    private Integer expeditionScore = 0;

    @Column(name = "guild_score", nullable = false)
    private Integer guildScore = 0;

    @Column(name = "total_score", nullable = false)
    private Integer totalScore = 0;

    @Column(length = 50)
    private String title;

    @Column(name = "rank_position")
    private Integer rankPosition;
}
