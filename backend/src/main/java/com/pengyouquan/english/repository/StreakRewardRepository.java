package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.StreakReward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StreakRewardRepository extends JpaRepository<StreakReward, Long> {
    List<StreakReward> findAllByOrderByDaysRequiredAsc();
}
