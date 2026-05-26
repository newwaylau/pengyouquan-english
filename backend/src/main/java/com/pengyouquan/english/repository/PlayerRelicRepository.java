package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.PlayerRelic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerRelicRepository extends JpaRepository<PlayerRelic, Long> {

    List<PlayerRelic> findByExpeditionId(Long expeditionId);

    void deleteByExpeditionId(Long expeditionId);
}
