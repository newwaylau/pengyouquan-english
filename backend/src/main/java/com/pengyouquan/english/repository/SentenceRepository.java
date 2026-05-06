package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.Sentence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

/**
 * 句子数据访问层
 */
@Repository
public interface SentenceRepository extends JpaRepository<Sentence, Long> {

    /** 按剧集ID查所有句子（仅未停用） */
    @Query("SELECT s FROM Sentence s WHERE s.showId = :showId AND s.isDisabled = false ORDER BY s.id")
    List<Sentence> findByShowIdOrderById(@Param("showId") Long showId);

    /** 按剧集ID分页查句子（仅未停用） */
    @Query("SELECT s FROM Sentence s WHERE s.showId = :showId AND s.isDisabled = false ORDER BY s.id")
    List<Sentence> findByShowIdOrderById(@Param("showId") Long showId, Pageable pageable);

    /** 按多个剧集ID查句子 */
    @Query("SELECT s FROM Sentence s WHERE s.showId IN :showIds AND s.isDisabled = false ORDER BY RANDOM()")
    List<Sentence> findByShowIdsRandom(@Param("showIds") List<Long> showIds);

    /** 随机抽取 N 条句子（不按剧集筛选） */
    @Query(value = "SELECT * FROM sentences WHERE is_disabled = 0 ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<Sentence> findRandom(@Param("limit") int limit);

    /** 从指定剧集中随机抽取 */
    @Query(value = "SELECT * FROM sentences WHERE show_id = :showId AND is_disabled = 0 ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<Sentence> findRandomByShowId(@Param("showId") Long showId, @Param("limit") int limit);

    /** 从多个剧集中随机抽取 */
    @Query(value = "SELECT * FROM sentences WHERE show_id IN :showIds AND is_disabled = 0 ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<Sentence> findRandomByShowIds(@Param("showIds") List<Long> showIds, @Param("limit") int limit);

    /** 排除已练过的，随机抽取 */
    @Query(value = "SELECT * FROM sentences WHERE id NOT IN :excludeIds AND is_disabled = 0 ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<Sentence> findRandomExcluding(@Param("excludeIds") List<Long> excludeIds, @Param("limit") int limit);

    /** 搜索句子（中英文模糊匹配） */
    @Query("SELECT s FROM Sentence s WHERE s.isDisabled = false AND s.text LIKE %:query%")
    List<Sentence> searchByText(@Param("query") String query);

    /** 统计某个剧集的句子数（仅未停用） */
    @Query("SELECT COUNT(s) FROM Sentence s WHERE s.showId = :showId AND s.isDisabled = false")
    long countByShowId(@Param("showId") Long showId);

    /** 统计多个剧集的句子数（仅未停用） */
    @Query("SELECT COUNT(s) FROM Sentence s WHERE s.showId IN :showIds AND s.isDisabled = false")
    long countByShowIds(@Param("showIds") List<Long> showIds);
}
