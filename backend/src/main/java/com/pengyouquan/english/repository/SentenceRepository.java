package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.Sentence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 句子数据访问层
 */
@Repository
public interface SentenceRepository extends JpaRepository<Sentence, Long> {

    /** 按剧集ID查所有句子 */
    List<Sentence> findByShowIdOrderById(Long showId);

    /** 按多个剧集ID查句子 */
    @Query("SELECT s FROM Sentence s WHERE s.showId IN :showIds ORDER BY RANDOM()")
    List<Sentence> findByShowIdsRandom(@Param("showIds") List<Long> showIds);

    /** 随机抽取 N 条句子（不按剧集筛选） */
    @Query(value = "SELECT * FROM sentences ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<Sentence> findRandom(@Param("limit") int limit);

    /** 从指定剧集中随机抽取 */
    @Query(value = "SELECT * FROM sentences WHERE show_id = :showId ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<Sentence> findRandomByShowId(@Param("showId") Long showId, @Param("limit") int limit);

    /** 从多个剧集中随机抽取 */
    @Query(value = "SELECT * FROM sentences WHERE show_id IN :showIds ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<Sentence> findRandomByShowIds(@Param("showIds") List<Long> showIds, @Param("limit") int limit);

    /** 排除已练过的，随机抽取 */
    @Query(value = "SELECT * FROM sentences WHERE id NOT IN :excludeIds ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<Sentence> findRandomExcluding(@Param("excludeIds") List<Long> excludeIds, @Param("limit") int limit);

    /** 搜索句子（中英文模糊匹配） */
    @Query("SELECT s FROM Sentence s WHERE s.text LIKE %:query%")
    List<Sentence> searchByText(@Param("query") String query);

    /** 统计某个剧集的句子数 */
    long countByShowId(Long showId);

    /** 统计多个剧集的句子数 */
    @Query("SELECT COUNT(s) FROM Sentence s WHERE s.showId IN :showIds")
    long countByShowIds(@Param("showIds") List<Long> showIds);
}
