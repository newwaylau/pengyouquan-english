package com.pengyouquan.english.controller;

import com.pengyouquan.english.dto.ApiResponse;
import com.pengyouquan.english.model.Notification;
import com.pengyouquan.english.repository.NotificationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 通知公开接口
 * 提供已发布通知的查询，无需登录即可访问
 */
@RestController
@RequestMapping("/api")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * 获取已发布的通知列表（公开）
     */
    @GetMapping("/notifications")
    public ApiResponse<List<Notification>> getPublishedNotifications() {
        List<Notification> list = notificationRepository.findByPublishedTrueOrderByCreatedAtDesc();
        return ApiResponse.success(list);
    }
}
