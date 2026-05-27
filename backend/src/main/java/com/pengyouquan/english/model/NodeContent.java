package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "node_content")
public class NodeContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer act;

    @Column(nullable = false)
    private Integer floor;

    @Column(name = "node_type", nullable = false, length = 20)
    private String nodeType;

    @Column(nullable = false)
    private Integer position = 0;

    @Column(length = 200)
    private String title = "";

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "connected_to", columnDefinition = "JSON")
    private String connectedTo;

    @Column(name = "is_boss_node", nullable = false)
    private Boolean isBossNode = false;

    @Column(name = "event_choices", columnDefinition = "JSON")
    private String eventChoices;

    @Column(name = "enemy_id")
    private Long enemyId;

    @Column(name = "boss_id")
    private Long bossId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
