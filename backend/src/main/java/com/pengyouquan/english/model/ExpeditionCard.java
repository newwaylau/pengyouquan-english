package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "expedition_cards")
public class ExpeditionCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_name", nullable = false, length = 100)
    private String cardName;

    @Column(name = "card_name_en", nullable = false, length = 200)
    private String cardNameEn;

    @Column(name = "card_type", nullable = false, length = 20)
    private String cardType;

    @Column(nullable = false, length = 20)
    private String rarity = "common";

    @Column(nullable = false)
    private Integer cost = 1;

    @Column(length = 500)
    private String description = "";

    @Column(name = "base_damage", nullable = false)
    private Integer baseDamage = 0;

    @Column(name = "base_block", nullable = false)
    private Integer baseBlock = 0;

    @Column(columnDefinition = "JSON")
    private String keywords;

    @Column(name = "upgrade_description", length = 500)
    private String upgradeDescription = "";

    @Column(name = "upgrade_damage", nullable = false)
    private Integer upgradeDamage = 0;

    @Column(name = "upgrade_block", nullable = false)
    private Integer upgradeBlock = 0;

    @Column(name = "is_x_cost", nullable = false)
    private Boolean isXCost = false;

    @Column(name = "show_id")
    private Long showId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
