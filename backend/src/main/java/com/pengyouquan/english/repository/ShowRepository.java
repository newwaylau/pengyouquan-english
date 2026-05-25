package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 剧集数据访问层
 */
@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {

    /** 获取所有剧集，按名称排序 */
    List<Show> findAllByOrderByImportedAtDesc();

    /** 按名称搜索剧集 */
    List<Show> findByNameContaining(String name);

/** 获取剧集总数 */
@Query("SELECT COUNT(s) FROM Show s")
long countAll();

/** 检查是否存在相同名称、季、集的剧集 */
boolean existsByNameAndSeasonAndEpisode(String name, Integer season, Integer episode);
}
