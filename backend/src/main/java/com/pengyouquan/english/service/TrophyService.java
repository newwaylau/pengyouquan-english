package com.pengyouquan.english.service;

import com.pengyouquan.english.model.TrophyTier;
import com.pengyouquan.english.model.UserStats;
import com.pengyouquan.english.repository.TrophyTierRepository;
import com.pengyouquan.english.repository.UserStatsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    /**
     * 更新奖杯（含段位保护 + 连胜加成）
     * @param userId 用户ID
     * @param change 奖杯变化量（正数=赢，负数=输）
     * @return 实际奖杯变化量
     */
    @Transactional
    public int updateTrophies(Long userId, int change) {
        UserStats stats = getOrCreateUserStats(userId);

        int actualChange = change;

        if (change > 0) {
            // 胜利：计算连胜加成
            int winStreak = stats.getWinStreak();
            int streakBonus = 0;
            if (winStreak >= 5) {
                streakBonus = 10; // 5连胜+10
            } else if (winStreak >= 3) {
                streakBonus = 5;  // 3连胜+5
            }
            actualChange = change + streakBonus;

            int newTrophies = stats.getTrophies() + actualChange;
            stats.setTrophies(newTrophies);

            // 更新最高奖杯记录
            if (newTrophies > stats.getBestTrophies()) {
                stats.setBestTrophies(newTrophies);
            }

            // 更新连胜
            stats.setWins(stats.getWins() + 1);
            stats.setWinStreak(winStreak + 1);

            // 升段时更新段位保护底线
            updateTierFloor(stats);

        } else if (change < 0) {
            // 失败：段位保护 - 不能低于段位保护底线
            int oldTrophies = stats.getTrophies();
            int tierFloor = stats.getTierFloor();
            int newTrophies = Math.max(tierFloor, oldTrophies + change);
            // 也不能低于0
            newTrophies = Math.max(0, newTrophies);
            actualChange = newTrophies - oldTrophies; // 实际减少量可能小于change

            stats.setTrophies(newTrophies);
            stats.setLosses(stats.getLosses() + 1);
            stats.setWinStreak(0); // 连胜被终结
        }

        userStatsRepository.save(stats);
        return actualChange;
    }

    /** 更新段位保护底线：升到新段位时，将底线设为当前段位的最小奖杯数 */
    private void updateTierFloor(UserStats stats) {
        int trophies = stats.getTrophies();
        TrophyTier currentTier = getTier(trophies);
        if (currentTier != null) {
            int newFloor = currentTier.getMinTrophies();
            if (newFloor > stats.getTierFloor()) {
                stats.setTierFloor(newFloor);
            }
        }
    }

    /** 获取奖杯数 */
    public int getTrophies(Long userId) {
        return getOrCreateUserStats(userId).getTrophies();
    }

    /** 获取段位保护底线 */
    public int getTierFloor(Long userId) {
        return getOrCreateUserStats(userId).getTierFloor();
    }

    /** 根据奖杯查询段位 */
    public TrophyTier getTier(int trophies) {
        return trophyTierRepository.findByTrophiesRange(trophies)
                .orElse(trophyTierRepository.findAllByOrderByMinTrophiesAsc().stream()
                        .findFirst().orElse(null));
    }

    /** 获取所有段位 */
    public List<TrophyTier> getAllTiers() {
        return trophyTierRepository.findAllByOrderByMinTrophiesAsc();
    }
}
