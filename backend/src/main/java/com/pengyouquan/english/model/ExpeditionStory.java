package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "expedition_stories")
public class ExpeditionStory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "show_id", nullable = false)
    private Long showId;

    @Column(name = "episode_season", nullable = false)
    private Integer episodeSeason;

    @Column(name = "episode_number", nullable = false)
    private Integer episodeNumber;

    @Column(name = "story_intro", nullable = false, columnDefinition = "TEXT")
    private String storyIntro;

    @Column(name = "node_stories", nullable = false, columnDefinition = "JSON")
    private String nodeStories;

    @Column(name = "boss_name")
    private String bossName;

    @Column(name = "boss_story", columnDefinition = "TEXT")
    private String bossStory;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
