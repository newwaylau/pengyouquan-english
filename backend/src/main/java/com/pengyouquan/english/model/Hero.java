package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "heroes")
public class Hero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "show_id", nullable = false)
    private Long showId;

    @Column(name = "name_cn", nullable = false, length = 50)
    private String nameCn;

    @Column(name = "name_en", nullable = false, length = 100)
    private String nameEn;

    @Column(nullable = false)
    private Integer health = 30;

    @Column(name = "skill_name_cn", length = 50)
    private String skillNameCn;

    @Column(name = "skill_name_en", length = 100)
    private String skillNameEn;

    @Column(name = "skill_description_cn", length = 200)
    private String skillDescriptionCn;

    @Column(name = "skill_description_en", length = 200)
    private String skillDescriptionEn;

    @Column(name = "skill_cooldown", nullable = false)
    private Integer skillCooldown = 3;

    @Column(name = "base_effect_json", columnDefinition = "JSON")
    private String baseEffectJson;

    @Column(name = "unlock_condition", length = 200)
    private String unlockCondition;
}
