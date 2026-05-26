package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.SeasonRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeasonRankingRepository extends JpaRepository<SeasonRanking, Long> {
    List<SeasonRanking> findBySeasonNumberOrderByTotalScoreDesc(Integer seasonNumber);
    Optional<SeasonRanking> findByUserIdAndSeasonNumber(Long userId, Integer seasonNumber);
}
