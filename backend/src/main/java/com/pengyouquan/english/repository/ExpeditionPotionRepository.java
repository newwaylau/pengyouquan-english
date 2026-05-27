package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.ExpeditionPotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpeditionPotionRepository extends JpaRepository<ExpeditionPotion, Long> {

    List<ExpeditionPotion> findByShowIdOrShowIdIsNull(Long showId);

    List<ExpeditionPotion> findByEffectType(String effectType);
}
