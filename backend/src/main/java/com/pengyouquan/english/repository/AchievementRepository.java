package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    List<Achievement> findAllByOrderBySortOrderAsc();
    List<Achievement> findByCategoryOrderBySortOrderAsc(String category);
}
