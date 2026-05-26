package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "user_equipment")
public class UserEquipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(nullable = false)
    private Integer level = 1;

    @Column(name = "bonus_stats", columnDefinition = "JSON")
    private String bonusStats;

    @Column(name = "reroll_count", nullable = false)
    private Integer rerollCount = 0;
}
