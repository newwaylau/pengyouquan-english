package com.pengyouquan.english.service;

import com.pengyouquan.english.model.Notification;
import com.pengyouquan.english.model.TrophyTier;
import com.pengyouquan.english.model.User;
import com.pengyouquan.english.model.UserStats;
import com.pengyouquan.english.repository.NotificationRepository;
import com.pengyouquan.english.repository.TrophyTierRepository;
import com.pengyouquan.english.repository.UserRepository;
import com.pengyouquan.english.repository.UserStatsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 赛季重置服务
 * 每月1号重置段位（从当前段位-3开始），发送赛季奖励邮件/通知
 */
@Service
public class SeasonResetService {

    private static final Logger log = LoggerFactory.getLogger(SeasonResetService.class);

    private final UserStatsRepository userStatsRepository;
    private final TrophyTierRepository trophyTierRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public SeasonResetService(UserStatsRepository userStatsRepository,
                              TrophyTierRepository trophyTierRepository,
                              UserRepository userRepository,
                              NotificationRepository notificationRepository) {
        this.userStatsRepository = userStatsRepository;
        this.trophyTierRepository = trophyTierRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    /**
     * 每月1号凌晨2点执行赛季重置
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    @Transactional
    public void executeSeasonReset() {
        LocalDate today = LocalDate.now();
        log.info("开始赛季重置: {}", today);

        List<TrophyTier> allTiers = trophyTierRepository.findAllByOrderByMinTrophiesAsc();
        List<UserStats> allStats = userStatsRepository.findAll();

        int resetCount = 0;
        for (UserStats stats : allStats) {
            if (stats.getTrophies() <= 0) continue;

            // 根据当前段位计算目标段位（-3）
            int currentTierIndex = getTierIndex(stats.getTrophies(), allTiers);
            int targetTierIndex = Math.max(0, currentTierIndex - 3);
            TrophyTier targetTier = allTiers.get(targetTierIndex);

            // 重置奖杯到目标段位起始
            int newTrophies = targetTier.getMinTrophies();
            stats.setTrophies(newTrophies);
            stats.setTierFloor(targetTier.getMinTrophies());
            userStatsRepository.save(stats);
            resetCount++;
        }

        // 发送赛季重置通知
        Notification notification = new Notification();
        notification.setTitle("🎊 赛季重置通知");
        notification.setContent("新赛季已经开始！所有玩家的段位已根据上赛季段位-3进行了重置。继续征战，冲击更高段位吧！");
        notification.setPublished(true);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);

        log.info("赛季重置完成，共重置 {} 个玩家", resetCount);
    }

    /** 获取当前奖杯对应的段位索引 */
    private int getTierIndex(int trophies, List<TrophyTier> tiers) {
        for (int i = tiers.size() - 1; i >= 0; i--) {
            if (trophies >= tiers.get(i).getMinTrophies()) {
                return i;
            }
        }
        return 0;
    }

    /**
     * 获取赛季倒计时（距离下个月1号的天数）
     */
    public int getDaysUntilNextSeason() {
        LocalDate today = LocalDate.now();
        LocalDate nextMonth = today.withDayOfMonth(1).plusMonths(1);
        return (int) java.time.temporal.ChronoUnit.DAYS.between(today, nextMonth);
    }
}
