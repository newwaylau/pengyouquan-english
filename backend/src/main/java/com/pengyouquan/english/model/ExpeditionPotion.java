package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "expedition_potions")
public class ExpeditionPotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name_cn", nullable = false, length = 50)
    private String nameCn;

    @Column(name = "name_en", nullable = false, length = 100)
    private String nameEn;

    @Column(nullable = false, length = 20)
    private String rarity = "common";

    @Column(name = "effect_type", nullable = false, length = 50)
    private String effectType;

    @Column(name = "effect_value", nullable = false)
    private Integer effectValue = 0;

    @Column(name = "description_cn", nullable = false, length = 300)
    private String descriptionCn;

    @Column(name = "description_en", nullable = false, length = 300)
    private String descriptionEn;

    @Column(length = 20)
    private String icon = "🧪";

    @Column(name = "show_id")
    private Long showId;

    @Column(nullable = false)
    private Integer price = 40;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
