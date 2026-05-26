package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "rank_tiers")
public class TrophyTier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name_cn", nullable = false, length = 50)
    private String nameCn;

    @Column(name = "name_en", nullable = false, length = 50)
    private String nameEn;

    @Column(name = "min_trophies", nullable = false)
    private Integer minTrophies;

    @Column(name = "max_trophies", nullable = false)
    private Integer maxTrophies;

    @Column(length = 10)
    private String icon = "";

    @Column(name = "season_reward_type", length = 20)
    private String seasonRewardType;

    @Column(name = "season_reward_count")
    private Integer seasonRewardCount = 0;
}
