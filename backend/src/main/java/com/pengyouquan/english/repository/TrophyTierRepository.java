package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.TrophyTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrophyTierRepository extends JpaRepository<TrophyTier, Long> {
    @Query("SELECT t FROM TrophyTier t WHERE t.minTrophies <= :trophies AND t.maxTrophies >= :trophies")
    Optional<TrophyTier> findByTrophiesRange(@Param("trophies") int trophies);

    List<TrophyTier> findAllByOrderByMinTrophiesAsc();
}
