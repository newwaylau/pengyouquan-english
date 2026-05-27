package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.ExpeditionStory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExpeditionStoryRepository extends JpaRepository<ExpeditionStory, Long> {

    Optional<ExpeditionStory> findByShowIdAndEpisodeSeasonAndEpisodeNumber(
            Long showId, Integer episodeSeason, Integer episodeNumber);
}
