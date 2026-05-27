package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.Deck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeckRepository extends JpaRepository<Deck, Long> {

    List<Deck> findByUserId(Long userId);

    Optional<Deck> findByUserIdAndIsActiveTrue(Long userId);
}
