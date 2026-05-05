package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.PracticeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 练习记录数据访问层
 */
@Repository
public interface PracticeLogRepository extends JpaRepository<PracticeLog, Long> {

    long countByUserId(Long userId);

    long countByUserIdAndPracticedAtAfter(Long userId, LocalDateTime after);

    long countByUserIdAndCorrect(Long userId, boolean correct);

    /** 管理员：按日期统计每日练习量 */
    @Query(value = "SELECT DATE(practiced_at) as day, COUNT(*) as cnt " +
           "FROM practice_logs " +
           "WHERE practiced_at >= :since " +
           "GROUP BY DATE(practiced_at) " +
           "ORDER BY day", nativeQuery = true)
    List<Object[]> dailyPracticeCount(@Param("since") LocalDateTime since);

    /** 管理员：按日期统计每日正确率 */
    @Query(value = "SELECT DATE(practiced_at) as day, " +
           "SUM(CASE WHEN correct = true THEN 1 ELSE 0 END) as correct, " +
           "COUNT(*) as total " +
           "FROM practice_logs " +
           "WHERE practiced_at >= :since " +
           "GROUP BY DATE(practiced_at) " +
           "ORDER BY day", nativeQuery = true)
    List<Object[]> dailyAccuracy(@Param("since") LocalDateTime since);

    /** 管理员：统计练习模式分布 */
    @Query(value = "SELECT mode, COUNT(*) as cnt FROM practice_logs GROUP BY mode", nativeQuery = true)
    List<Object[]> modeDistribution();

    /** 管理员：统计总练习量 */
    long count();
}
