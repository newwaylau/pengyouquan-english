package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.StardustLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StardustLogRepository extends JpaRepository<StardustLog, Long> {
    List<StardustLog> findByUserIdOrderByCreatedAtDesc(Long userId);
}
