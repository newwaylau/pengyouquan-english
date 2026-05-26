package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "cards")
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "show_id", nullable = false)
    private Long showId;

    @Column(name = "name_cn", nullable = false, length = 100)
    private String nameCn;

    @Column(name = "name_en", nullable = false, length = 200)
    private String nameEn;

    @Column(name = "card_type", nullable = false, length = 20)
    private String cardType;

    @Column(nullable = false, length = 20)
    private String rarity = "common";

    @Column(nullable = false)
    private Integer cost = 0;

    private Integer attack;

    private Integer health;

    @Column(name = "effect_json", columnDefinition = "TEXT")
    private String effectJson;

    @Column(name = "challenge_type", length = 20)
    private String challengeType;

    @Column(name = "challenge_sentence_id")
    private Long challengeSentenceId;

    @Column(length = 50)
    private String faction;

    @Column(name = "quote_text", length = 500)
    private String quoteText;

    @Column(name = "has_golden")
    private Boolean hasGolden = false;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
