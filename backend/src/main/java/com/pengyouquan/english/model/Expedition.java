package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "expeditions")
public class Expedition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "show_id", nullable = false)
    private Long showId;

    @Column(nullable = false)
    private Integer act = 1;

    @Column(nullable = false)
    private Integer node = 1;

    @Column(name = "max_act", nullable = false)
    private Integer maxAct = 1;

    @Column(name = "player_hp", nullable = false)
    private Integer playerHp = 30;

    @Column(name = "max_hp", nullable = false)
    private Integer maxHp = 30;

    @Column(name = "starting_deck", columnDefinition = "JSON", nullable = false)
    private String startingDeck;

    @Column(name = "current_deck", columnDefinition = "JSON", nullable = false)
    private String currentDeck;

    @Column(columnDefinition = "JSON")
    private String relics;

    @Column(nullable = false)
    private Integer gold = 0;

    @Column(nullable = false, length = 20)
    private String status = "in_progress";

    @Column(name = "questions_answered", nullable = false)
    private Integer questionsAnswered = 0;

    @Column(name = "questions_total", nullable = false)
    private Integer questionsTotal = 0;

    @Column(name = "enemies_killed", nullable = false)
    private Integer enemiesKilled = 0;

    @Column(name = "current_enemy_id")
    private Long currentEnemyId;

    @Column(name = "current_enemy_hp")
    private Integer currentEnemyHp;

    @Column(name = "map_nodes", columnDefinition = "JSON")
    private String mapNodes;

    @Column(name = "battle_state", columnDefinition = "JSON")
    private String battleState;

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
