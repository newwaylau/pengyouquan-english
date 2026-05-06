package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.WrongSentence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 错题数据访问层
 */
@Repository
public interface WrongSentenceRepository extends JpaRepository<WrongSentence, Long> {
    long countByUserId(Long userId);

    Optional<WrongSentence> findByUserIdAndSentenceId(Long userId, Long sentenceId);

    /** 按最后练习时间降序（旧版，保留兼容） */
    List<WrongSentence> findByUserIdOrderByLastPracticedAtDesc(Long userId);

    /** 按错误次数降序（高频优先） */
    List<WrongSentence> findByUserIdOrderByErrorCountDesc(Long userId);

    /** 按剧集名筛选 */
    List<WrongSentence> findByUserIdAndShowNameOrderByErrorCountDesc(Long userId, String showName);

    /** 获取未掌握的错题（用于批量练习） */
    List<WrongSentence> findByUserIdAndIsMasteredFalseOrderByErrorCountDesc(Long userId);

    /** 按剧集名筛选未掌握的错题 */
    List<WrongSentence> findByUserIdAndShowNameAndIsMasteredFalseOrderByErrorCountDesc(
            Long userId, String showName);

    /** 获取用户错题中所有不同的剧集名 */
    @Query("SELECT DISTINCT ws.showName FROM WrongSentence ws WHERE ws.userId = :userId AND ws.showName != ''")
    List<String> findDistinctShowNamesByUserId(@Param("userId") Long userId);

    /** 获取今天要复习的错题（next_review_at <= now 或为null，且未掌握） */
    @Query("SELECT ws FROM WrongSentence ws WHERE ws.userId = :userId " +
           "AND ws.isMastered = false " +
           "AND (ws.nextReviewAt IS NULL OR ws.nextReviewAt <= :now) " +
           "ORDER BY ws.nextReviewAt ASC")
    List<WrongSentence> findDueByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /** 获取以后才需要复习的错题 */
    @Query("SELECT ws FROM WrongSentence ws WHERE ws.userId = :userId " +
           "AND ws.isMastered = false " +
           "AND ws.nextReviewAt IS NOT NULL AND ws.nextReviewAt > :now " +
           "ORDER BY ws.nextReviewAt ASC")
    List<WrongSentence> findUpcomingByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Transactional
    void deleteByUserId(Long userId);

    @Transactional
    void deleteByUserIdAndSentenceId(Long userId, Long sentenceId);
}
