package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.SeasonConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SeasonConfigRepository extends JpaRepository<SeasonConfig, Long> {

    Optional<SeasonConfig> findBySeasonNumber(Integer seasonNumber);

    Optional<SeasonConfig> findByIsActiveTrue();
}
