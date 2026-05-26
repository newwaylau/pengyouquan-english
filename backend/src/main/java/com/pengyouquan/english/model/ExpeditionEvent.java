package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "expedition_events")
public class ExpeditionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "show_id", nullable = false)
    private Long showId;

    @Column(nullable = false)
    private Integer act;

    @Column(name = "title_cn", nullable = false, length = 100)
    private String titleCn;

    @Column(name = "description_cn", columnDefinition = "TEXT", nullable = false)
    private String descriptionCn;

    @Column(columnDefinition = "JSON", nullable = false)
    private String choices;
}
