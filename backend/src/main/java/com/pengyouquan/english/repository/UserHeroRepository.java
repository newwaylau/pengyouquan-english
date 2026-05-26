package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.UserHero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserHeroRepository extends JpaRepository<UserHero, Long> {

    List<UserHero> findByUserId(Long userId);

    Optional<UserHero> findByUserIdAndIsActiveTrue(Long userId);

    Optional<UserHero> findByUserIdAndHeroId(Long userId, Long heroId);
}
