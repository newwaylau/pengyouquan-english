package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.ExpeditionRelic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpeditionRelicRepository extends JpaRepository<ExpeditionRelic, Long> {

    List<ExpeditionRelic> findByShowIdOrShowIdIsNull(Long showId);

    List<ExpeditionRelic> findByRarity(String rarity);

    List<ExpeditionRelic> findByEffectType(String effectType);
}
