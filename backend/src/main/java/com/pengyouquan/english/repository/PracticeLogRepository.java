package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.PracticeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * 练习记录数据访问层
 */
@Repository
public interface PracticeLogRepository extends JpaRepository<PracticeLog, Long> {

    long countByUserId(Long userId);

    long countByUserIdAndPracticedAtAfter(Long userId, LocalDateTime after);

    long countByUserIdAndCorrect(Long userId, boolean correct);
}
