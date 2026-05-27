package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.ExpeditionCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpeditionCardRepository extends JpaRepository<ExpeditionCard, Long> {

    List<ExpeditionCard> findByRarity(String rarity);

    List<ExpeditionCard> findByCardType(String cardType);

    List<ExpeditionCard> findByShowId(Long showId);
}
