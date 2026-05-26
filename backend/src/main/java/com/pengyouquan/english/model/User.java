package com.pengyouquan.english.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户实体
 * 对应数据库 users 表
 */
@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(length = 20)
    private String phone = "";

    @Column(nullable = false)
    private String password;

    @Column(length = 100)
    private String nickname = "";

    @Column(length = 500)
    private String avatar = "";

    @Column(name = "wechat_open_id")
    private String wechatOpenId;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(nullable = false)
    private String role = "user";

    @Column(name = "invited_by", length = 100)
    private String invitedBy = "";

    @Column(name = "invite_code", length = 20, unique = true)
    private String inviteCode;

    @Column(nullable = false)
    private Integer prestige = 0;

    @Column(name = "rank_tier", nullable = false)
    private Integer rankTier = 1;

    @Column(name = "consecutive_days", nullable = false)
    private Integer consecutiveDays = 0;

    @Column(name = "last_daily_date")
    private LocalDate lastDailyDate;

    @Column(nullable = false)
    private Integer stardust = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
