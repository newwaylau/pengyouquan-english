package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.BattleHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BattleHistoryRepository extends JpaRepository<BattleHistory, Long> {
    List<BattleHistory> findByChallengerIdOrDefenderIdOrderByCreatedAtDesc(Long challengerId, Long defenderId);

    Optional<BattleHistory> findByIdAndStatus(Long id, String status);

    @Query("SELECT b FROM BattleHistory b WHERE b.defenderId = :defenderId AND b.status = :status ORDER BY b.createdAt DESC")
    List<BattleHistory> findByDefenderIdAndStatusOrderByCreatedAtDesc(@Param("defenderId") Long defenderId, @Param("status") String status);

    @Query("SELECT b FROM BattleHistory b WHERE (b.challengerId = :userId OR b.defenderId = :userId) AND b.status = 'completed' ORDER BY b.createdAt DESC")
    List<BattleHistory> findCompletedByUserId(@Param("userId") Long userId);
}
