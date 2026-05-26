package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.GuildMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuildMemberRepository extends JpaRepository<GuildMember, Long> {

    List<GuildMember> findByGuildId(Long guildId);

    Optional<GuildMember> findByUserId(Long userId);

    Optional<GuildMember> findByGuildIdAndUserId(Long guildId, Long userId);

    List<GuildMember> findTop3ByGuildIdOrderByWeeklyScoreDesc(Long guildId);

    long countByGuildId(Long guildId);
}
