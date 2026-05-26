package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.Expedition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpeditionRepository extends JpaRepository<Expedition, Long> {

    Optional<Expedition> findByUserIdAndStatus(Long userId, String status);

    Optional<Expedition> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    List<Expedition> findByUserIdAndStatusNotOrderByCreatedAtDesc(Long userId, String status);
}
