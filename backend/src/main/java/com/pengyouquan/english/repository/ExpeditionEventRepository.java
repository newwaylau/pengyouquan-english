package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.ExpeditionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpeditionEventRepository extends JpaRepository<ExpeditionEvent, Long> {

    List<ExpeditionEvent> findByShowIdAndAct(Long showId, Integer act);
}
