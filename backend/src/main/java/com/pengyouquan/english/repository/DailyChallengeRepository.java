package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.DailyChallenge;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyChallengeRepository extends JpaRepository<DailyChallenge, Long> {

    Optional<DailyChallenge> findByUserIdAndChallengeDate(Long userId, LocalDate challengeDate);

    List<DailyChallenge> findByUserIdAndChallengeDateBetweenOrderByChallengeDateDesc(Long userId, LocalDate start, LocalDate end);

    List<DailyChallenge> findByCompletedAndChallengeDateBetween(Boolean completed, LocalDate start, LocalDate end);

    List<DailyChallenge> findByUserIdAndCompletedTrueOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
