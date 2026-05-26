package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.HeroGear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HeroGearRepository extends JpaRepository<HeroGear, Long> {

    Optional<HeroGear> findByUserId(Long userId);
}
