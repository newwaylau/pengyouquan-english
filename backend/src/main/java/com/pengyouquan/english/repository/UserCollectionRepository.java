package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.UserCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserCollectionRepository extends JpaRepository<UserCollection, Long> {

    List<UserCollection> findByUserId(Long userId);

    Optional<UserCollection> findByUserIdAndCardId(Long userId, Long cardId);
}
