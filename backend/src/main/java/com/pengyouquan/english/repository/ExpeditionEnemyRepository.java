package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.ExpeditionEnemy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpeditionEnemyRepository extends JpaRepository<ExpeditionEnemy, Long> {

    List<ExpeditionEnemy> findByShowIdAndAct(Long showId, Integer act);

    List<ExpeditionEnemy> findByShowIdAndActAndIsBossTrue(Long showId, Integer act);
}
