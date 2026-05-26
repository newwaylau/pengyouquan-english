package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.Guild;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuildRepository extends JpaRepository<Guild, Long> {

    Optional<Guild> findByName(String name);

    Optional<Guild> findByLeaderId(Long leaderId);

    List<Guild> findByNameContainingIgnoreCase(String query);

    List<Guild> findAllByOrderByWeeklyScoreDesc();

    @Query("SELECT g FROM Guild g ORDER BY g.rankPoints DESC")
    List<Guild> findAllByOrderByRankPointsDesc();
}
