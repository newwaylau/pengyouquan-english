package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.SentenceFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 句子举报数据访问层
 */
@Repository
public interface SentenceFlagRepository extends JpaRepository<SentenceFlag, Long> {

    /** 查用户对某个句子的举报记录 */
    Optional<SentenceFlag> findBySentenceIdAndUserId(Long sentenceId, Long userId);

    /** 查某个句子被举报的次数 */
    long countBySentenceId(Long sentenceId);

    /** 获取所有被举报的句子 ID 及对应举报数 */
    @Query("SELECT sf.sentenceId, COUNT(sf) FROM SentenceFlag sf GROUP BY sf.sentenceId ORDER BY COUNT(sf) DESC")
    List<Object[]> findFlaggedSentenceIds();

    /** 删除用户对某个句子的举报 */
    void deleteBySentenceIdAndUserId(Long sentenceId, Long userId);
}
