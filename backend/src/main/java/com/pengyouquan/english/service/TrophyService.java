package com.pengyouquan.english.service;

import com.pengyouquan.english.model.TrophyTier;
import com.pengyouquan.english.model.UserStats;
import com.pengyouquan.english.repository.TrophyTierRepository;
import com.pengyouquan.english.repository.UserStatsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrophyService {

    private final UserStatsRepository userStatsRepository;
    private final TrophyTierRepository trophyTierRepository;

    public TrophyService(UserStatsRepository userStatsRepository, TrophyTierRepository trophyTierRepository) {
        this.userStatsRepository = userStatsRepository;
        this.trophyTierRepository = trophyTierRepository;
    }

    /** 获取或创建用户统计数据 */
    @Transactional
    public UserStats getOrCreateUserStats(Long userId) {
        return userStatsRepository.findById(userId)
                .orElseGet(() -> {
                    UserStats stats = new UserStats();
                    stats.setUserId(userId);
                    return userStatsRepository.save(stats);
                });
    }

    /** 更新奖杯 */
    @Transactional
    public void updateTrophies(Long userId, int change) {
        UserStats stats = getOrCreateUserStats(userId);
        int newTrophies = Math.max(0, stats.getTrophies() + change);
        stats.setTrophies(newTrophies);
        if (newTrophies > stats.getBestTrophies()) {
            stats.setBestTrophies(newTrophies);
        }
        if (change > 0) {
            stats.setWins(stats.getWins() + 1);
            stats.setWinStreak(stats.getWinStreak() + 1);
        } else if (change < 0) {
            stats.setLosses(stats.getLosses() + 1);
            stats.setWinStreak(0);
        }
        userStatsRepository.save(stats);
    }

    /** 获取奖杯数 */
    public int getTrophies(Long userId) {
        return getOrCreateUserStats(userId).getTrophies();
    }

    /** 根据奖杯查询段位 */
    public TrophyTier getTier(int trophies) {
        return trophyTierRepository.findByTrophiesRange(trophies)
                .orElse(trophyTierRepository.findAllByOrderByMinTrophiesAsc().stream()
                        .findFirst().orElse(null));
    }
}
