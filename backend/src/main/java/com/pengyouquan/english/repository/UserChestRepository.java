package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.UserChest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserChestRepository extends JpaRepository<UserChest, Long> {

    List<UserChest> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<UserChest> findByUserIdAndStatusOrderByCreatedAtAsc(Long userId, String status);

    int countByUserIdAndStatusAndChestType(Long userId, String status, String chestType);

    Optional<UserChest> findTopByUserIdAndStatusOrderByCreatedAtAsc(Long userId, String status);
}
