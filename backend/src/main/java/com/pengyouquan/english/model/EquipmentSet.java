package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "equipment_sets")
public class EquipmentSet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "set_key", nullable = false, unique = true, length = 50)
    private String setKey;

    @Column(name = "name_cn", nullable = false, length = 50)
    private String nameCn;

    @Column(name = "two_piece_effect_cn", length = 200)
    private String twoPieceEffectCn;

    @Column(name = "five_piece_effect_cn", length = 200)
    private String fivePieceEffectCn;

    @Column(name = "two_piece_effect_json", columnDefinition = "JSON")
    private String twoPieceEffectJson;

    @Column(name = "five_piece_effect_json", columnDefinition = "JSON")
    private String fivePieceEffectJson;
}
