package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.GuildWar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuildWarRepository extends JpaRepository<GuildWar, Long> {
    List<GuildWar> findByGuildIdAndPhase(Long guildId, String phase);
    Optional<GuildWar> findByGuildIdAndWeekNumber(Long guildId, Integer weekNumber);
    List<GuildWar> findByPhaseOrderByStartDateAsc(String phase);
    List<GuildWar> findByPhase(String phase);
    Optional<GuildWar> findTopByGuildIdOrderByWeekNumberDesc(Long guildId);
}
