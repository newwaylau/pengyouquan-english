package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.UserCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserCardRepository extends JpaRepository<UserCard, Long> {

    List<UserCard> findByUserId(Long userId);

    Optional<UserCard> findByUserIdAndCardId(Long userId, Long cardId);
}
