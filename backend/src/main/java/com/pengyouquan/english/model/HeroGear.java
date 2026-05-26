package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "hero_gear")
public class HeroGear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "weapon_id")
    private Long weaponId;

    @Column(name = "armor_id")
    private Long armorId;

    @Column(name = "trinket_id")
    private Long trinketId;

    @Column(name = "tome_id")
    private Long tomeId;

    @Column(name = "crown_id")
    private Long crownId;
}
