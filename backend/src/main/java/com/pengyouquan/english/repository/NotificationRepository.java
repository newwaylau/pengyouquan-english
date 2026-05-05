package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 系统通知数据访问层
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** 查询所有已发布通知，按创建时间降序排列 */
    List<Notification> findByPublishedTrueOrderByCreatedAtDesc();
}
