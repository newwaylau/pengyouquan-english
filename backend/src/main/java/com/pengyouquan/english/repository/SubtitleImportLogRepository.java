package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.SubtitleImportLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 字幕导入记录数据访问层
 */
@Repository
public interface SubtitleImportLogRepository extends JpaRepository<SubtitleImportLog, Long> {

    /** 按剧集ID查询导入记录，按时间降序 */
    List<SubtitleImportLog> findByShowIdOrderByImportedAtDesc(Long showId);

    /** 查询所有导入记录，按时间降序 */
    List<SubtitleImportLog> findAllByOrderByImportedAtDesc();
}
