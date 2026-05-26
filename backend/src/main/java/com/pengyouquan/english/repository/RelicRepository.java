package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.Relic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RelicRepository extends JpaRepository<Relic, Long> {

    List<Relic> findBySource(String source);

    List<Relic> findAllByOrderByRarityAsc();
}
