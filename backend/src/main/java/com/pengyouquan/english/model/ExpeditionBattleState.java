package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "expedition_battle_state")
public class ExpeditionBattleState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expedition_id", nullable = false)
    private Long expeditionId;

    @Column(name = "enemy_id", nullable = false)
    private Long enemyId;

    @Column(name = "enemy_hp", nullable = false)
    private Integer enemyHp;

    @Column(name = "enemy_max_hp", nullable = false)
    private Integer enemyMaxHp;

    @Column(name = "enemy_block", nullable = false)
    private Integer enemyBlock = 0;

    @Column(name = "enemy_buffs", columnDefinition = "JSON")
    private String enemyBuffs;

    @Column(name = "player_buffs", columnDefinition = "JSON")
    private String playerBuffs;

    @Column(name = "turn_number", nullable = false)
    private Integer turnNumber = 1;

    @Column(name = "hand_cards", nullable = false, columnDefinition = "JSON")
    private String handCards;

    @Column(name = "draw_pile", nullable = false, columnDefinition = "JSON")
    private String drawPile;

    @Column(name = "discard_pile", nullable = false, columnDefinition = "JSON")
    private String discardPile;

    @Column(nullable = false)
    private Integer energy = 3;

    @Column(name = "max_energy", nullable = false)
    private Integer maxEnergy = 3;

    @Column(name = "player_block", nullable = false)
    private Integer playerBlock = 0;

    @Column(nullable = false, length = 20)
    private String status = "fighting";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
