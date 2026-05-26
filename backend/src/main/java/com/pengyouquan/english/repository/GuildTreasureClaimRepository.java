package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.GuildTreasureClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GuildTreasureClaimRepository extends JpaRepository<GuildTreasureClaim, Long> {

    Optional<GuildTreasureClaim> findByTreasureIdAndUserId(Long treasureId, Long userId);

    long countByTreasureId(Long treasureId);
}
