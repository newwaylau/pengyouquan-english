package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "expedition_enemies")
public class ExpeditionEnemy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "show_id", nullable = false)
    private Long showId;

    @Column(nullable = false)
    private Integer act;

    @Column(name = "name_cn", nullable = false, length = 50)
    private String nameCn;

    @Column(name = "name_en", nullable = false, length = 100)
    private String nameEn;

    @Column(nullable = false)
    private Integer hp;

    @Column(name = "is_boss", nullable = false)
    private Boolean isBoss = false;

    @Column(name = "special_rules", columnDefinition = "JSON")
    private String specialRules;

    @Column(name = "reward_pool", columnDefinition = "JSON")
    private String rewardPool;
}
