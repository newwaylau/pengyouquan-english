package com.pengyouquan.english.service;

import com.pengyouquan.english.model.User;
import com.pengyouquan.english.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class GoldService {

    private static final Logger log = LoggerFactory.getLogger(GoldService.class);

    private final UserRepository userRepository;

    public GoldService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** 查询金币余额 */
    public int getBalance(Long userId) {
        return userRepository.findById(userId)
                .map(u -> u.getGold() != null ? u.getGold() : 0)
                .orElse(0);
    }

    /** 增加金币 */
    public int addGold(Long userId, int amount) {
        return userRepository.findById(userId).map(u -> {
            int current = u.getGold() != null ? u.getGold() : 0;
            u.setGold(current + amount);
            userRepository.save(u);
            log.info("User {} gold: {} + {} = {}", userId, current, amount, current + amount);
            return current + amount;
        }).orElse(0);
    }

    /** 扣除金币（返回是否成功） */
    public boolean deductGold(Long userId, int amount) {
        return userRepository.findById(userId).map(u -> {
            int current = u.getGold() != null ? u.getGold() : 0;
            if (current < amount) return false;
            u.setGold(current - amount);
            userRepository.save(u);
            log.info("User {} gold: {} - {} = {}", userId, current, amount, current - amount);
            return true;
        }).orElse(false);
    }

    /** 检查今日是否已有首胜 */
    public boolean hasDailyWinToday(Long userId) {
        // 检查 battle_history 表中今天是否有该用户的胜利记录
        // 由于没有 BattleHistory repository 引用，在 controller 中实现
        return false;
    }

    /** 增加每日首胜金币 */
    public int addDailyWinBonus(Long userId) {
        return addGold(userId, 20);
    }
}
