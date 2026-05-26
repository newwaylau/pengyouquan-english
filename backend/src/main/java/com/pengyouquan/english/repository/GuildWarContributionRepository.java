package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.GuildWarContribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuildWarContributionRepository extends JpaRepository<GuildWarContribution, Long> {
    Optional<GuildWarContribution> findByWarIdAndUserId(Long warId, Long userId);
    List<GuildWarContribution> findByWarId(Long warId);
}
