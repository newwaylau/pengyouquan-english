package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.WrongSentence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 错题数据访问层
 */
@Repository
public interface WrongSentenceRepository extends JpaRepository<WrongSentence, Long> {
    long countByUserId(Long userId);

    Optional<WrongSentence> findByUserIdAndSentenceId(Long userId, Long sentenceId);

    List<WrongSentence> findByUserIdOrderByLastPracticedAtDesc(Long userId);

    @Transactional
    void deleteByUserId(Long userId);

    @Transactional
    void deleteByUserIdAndSentenceId(Long userId, Long sentenceId);
}
