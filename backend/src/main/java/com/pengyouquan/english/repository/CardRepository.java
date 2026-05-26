package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    List<Card> findByShowId(Long showId);

    List<Card> findByRarity(String rarity);
}
