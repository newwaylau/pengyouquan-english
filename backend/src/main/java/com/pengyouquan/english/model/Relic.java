package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "relics")
public class Relic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name_cn", nullable = false, length = 50)
    private String nameCn;

    @Column(name = "name_en", nullable = false, length = 100)
    private String nameEn;

    @Column(nullable = false, length = 20)
    private String rarity;

    @Column(name = "effect_cn", nullable = false, length = 200)
    private String effectCn;

    @Column(name = "effect_json", columnDefinition = "JSON", nullable = false)
    private String effectJson;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(name = "show_id")
    private Long showId;
}
