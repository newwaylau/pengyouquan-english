package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "achievements")
public class Achievement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(name = "key_name", nullable = false, unique = true, length = 50)
    private String keyName;

    @Column(name = "name_cn", nullable = false, length = 100)
    private String nameCn;

    @Column(name = "description_cn", nullable = false, length = 200)
    private String descriptionCn;

    @Column(length = 50)
    private String icon = "";

    @Column(nullable = false, length = 20)
    private String rarity = "common";

    @Column(name = "condition_type", nullable = false, length = 50)
    private String conditionType;

    @Column(name = "condition_value", nullable = false)
    private Integer conditionValue;

    @Column(name = "reward_stardust", nullable = false)
    private Integer rewardStardust = 0;

    @Column(name = "reward_card_id")
    private Long rewardCardId;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
