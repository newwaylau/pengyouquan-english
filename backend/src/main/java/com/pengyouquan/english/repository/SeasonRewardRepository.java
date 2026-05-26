package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.SeasonReward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeasonRewardRepository extends JpaRepository<SeasonReward, Long> {

    List<SeasonReward> findByUserId(Long userId);

    Optional<SeasonReward> findByUserIdAndSeasonNumber(Long userId, Integer seasonNumber);

    List<SeasonReward> findBySeasonNumber(Integer seasonNumber);
}
