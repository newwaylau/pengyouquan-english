package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "player_relics", indexes = {
    @Index(name = "idx_expedition", columnList = "expedition_id")
})
public class PlayerRelic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expedition_id", nullable = false)
    private Long expeditionId;

    @Column(name = "relic_id", nullable = false)
    private Long relicId;
}
