package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.ExpeditionBattleState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExpeditionBattleStateRepository extends JpaRepository<ExpeditionBattleState, Long> {

    Optional<ExpeditionBattleState> findByExpeditionId(Long expeditionId);

    void deleteByExpeditionId(Long expeditionId);
}
