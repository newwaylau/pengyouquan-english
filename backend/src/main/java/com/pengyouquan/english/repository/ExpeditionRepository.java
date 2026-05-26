package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.Expedition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ExpeditionRepository extends JpaRepository<Expedition, Long> {

    Optional<Expedition> findByUserIdAndStatus(Long userId, String status);

    Optional<Expedition> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    List<Expedition> findByUserIdAndStatusNotOrderByCreatedAtDesc(Long userId, String status);

    long countByUserIdAndStatus(Long userId, String status);

    @Query(value = "SELECT COALESCE(SUM(CASE WHEN e.status = 'cleared' THEN 3 WHEN e.max_act >= 3 THEN 2 WHEN e.max_act >= 2 THEN 1 ELSE 0 END), 0) FROM expeditions e WHERE e.user_id = :userId AND e.status != 'in_progress'", nativeQuery = true)
    int sumBossKillsByUserId(@Param("userId") Long userId);
}
