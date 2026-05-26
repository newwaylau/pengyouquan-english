package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.GuildLeagueSeason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GuildLeagueSeasonRepository extends JpaRepository<GuildLeagueSeason, Long> {
    Optional<GuildLeagueSeason> findTopByOrderBySeasonNumberDesc();
    Optional<GuildLeagueSeason> findBySeasonNumber(Integer seasonNumber);
}
