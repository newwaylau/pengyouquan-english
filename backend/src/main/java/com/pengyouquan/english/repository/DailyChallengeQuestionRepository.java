package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.DailyChallengeQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DailyChallengeQuestionRepository extends JpaRepository<DailyChallengeQuestion, Long> {

    List<DailyChallengeQuestion> findByChallengeIdOrderById(Long challengeId);
}
