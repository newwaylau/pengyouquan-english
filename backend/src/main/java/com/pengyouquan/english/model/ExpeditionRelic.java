package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "expedition_relics")
public class ExpeditionRelic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name_cn", nullable = false, length = 50)
    private String nameCn;

    @Column(name = "name_en", nullable = false, length = 100)
    private String nameEn;

    @Column(nullable = false, length = 20)
    private String rarity;

    @Column(name = "effect_type", nullable = false, length = 50)
    private String effectType;

    @Column(name = "effect_value", nullable = false)
    private Integer effectValue;

    @Column(name = "description_cn", nullable = false, length = 300)
    private String descriptionCn;

    @Column(name = "description_en", nullable = false, length = 300)
    private String descriptionEn;

    @Column(nullable = false, length = 20)
    private String icon = "🪙";

    @Column(name = "show_id")
    private Long showId;
}
