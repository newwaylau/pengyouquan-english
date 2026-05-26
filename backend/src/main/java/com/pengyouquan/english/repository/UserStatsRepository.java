package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.UserStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserStatsRepository extends JpaRepository<UserStats, Long> {
    List<UserStats> findAllByOrderByTrophiesDesc();
}
