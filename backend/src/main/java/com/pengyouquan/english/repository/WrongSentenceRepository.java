package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.WrongSentence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 错题数据访问层
 */
@Repository
public interface WrongSentenceRepository extends JpaRepository<WrongSentence, Long> {
    long countByUserId(Long userId);
}
