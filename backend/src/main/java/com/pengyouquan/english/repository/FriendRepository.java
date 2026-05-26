package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.Friend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRepository extends JpaRepository<Friend, Long> {
    List<Friend> findByUserId(Long userId);
    List<Friend> findByFriendIdAndStatus(Long friendId, String status);
    Optional<Friend> findByUserIdAndFriendId(Long userId, Long friendId);
    Optional<Friend> findByUserIdAndFriendIdAndStatus(Long userId, Long friendId, String status);
}
