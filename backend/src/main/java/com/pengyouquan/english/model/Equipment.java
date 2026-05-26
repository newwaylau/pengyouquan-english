package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "equipment")
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name_cn", nullable = false, length = 100)
    private String nameCn;

    @Column(name = "name_en", nullable = false, length = 200)
    private String nameEn;

    @Column(nullable = false, length = 20)
    private String slot;

    @Column(nullable = false, length = 20)
    private String rarity = "common";

    @Column(name = "stat_bonus", columnDefinition = "JSON")
    private String statBonus;

    @Column(name = "effect_json", columnDefinition = "JSON")
    private String effectJson;

    @Column(name = "affinity_show_id")
    private Long affinityShowId;

    @Column(name = "unlock_condition", length = 200)
    private String unlockCondition;
}
