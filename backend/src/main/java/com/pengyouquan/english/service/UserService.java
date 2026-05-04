package com.pengyouquan.english.service;

import com.pengyouquan.english.config.GlobalExceptionHandler.BusinessException;
import com.pengyouquan.english.dto.UpdateProfileRequest;
import com.pengyouquan.english.dto.UserInfoResponse;
import com.pengyouquan.english.dto.UserStatsResponse;
import com.pengyouquan.english.model.User;
import com.pengyouquan.english.repository.PracticeLogRepository;
import com.pengyouquan.english.repository.UserRepository;
import com.pengyouquan.english.repository.WrongSentenceRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 用户服务
 * 资料修改/统计/搜索/禁用/删除
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PracticeLogRepository practiceLogRepository;
    private final WrongSentenceRepository wrongSentenceRepository;

    public UserService(UserRepository userRepository,
                       PracticeLogRepository practiceLogRepository,
                       WrongSentenceRepository wrongSentenceRepository) {
        this.userRepository = userRepository;
        this.practiceLogRepository = practiceLogRepository;
        this.wrongSentenceRepository = wrongSentenceRepository;
    }

    /** 修改个人资料 */
    public UserInfoResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        if (request.getNickname() != null) user.setNickname(request.getNickname());
        if (request.getAvatar() != null) user.setAvatar(request.getAvatar());
        userRepository.save(user);
        return UserInfoResponse.fromUser(user);
    }

    /** 获取用户统计 */
    public UserStatsResponse getUserStats(Long userId) {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);

        long total = practiceLogRepository.countByUserId(userId);
        long today = practiceLogRepository.countByUserIdAndPracticedAtAfter(userId, todayStart);
        long correct = practiceLogRepository.countByUserIdAndCorrect(userId, true);
        long wrong = wrongSentenceRepository.countByUserId(userId);
        double accuracy = total > 0 ? (double) correct / total * 100 : 0;

        return new UserStatsResponse(total, today, correct, wrong, Math.round(accuracy * 100.0) / 100.0);
    }

    /** 搜索用户（管理员用） */
    public List<UserInfoResponse> searchUsers(String query) {
        return userRepository.findAll().stream()
                .filter(u -> u.getEmail().contains(query) || u.getNickname().contains(query))
                .map(UserInfoResponse::fromUser)
                .toList();
    }

    /** 禁用/解禁用户 */
    public void toggleEnabled(Long targetId) {
        User user = userRepository.findById(targetId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        user.setEnabled(!user.getEnabled());
        userRepository.save(user);
    }

    /** 删除用户 */
    public void deleteUser(Long targetId) {
        if (!userRepository.existsById(targetId)) {
            throw new BusinessException("用户不存在");
        }
        userRepository.deleteById(targetId);
    }

    /** 检查管理员权限 */
    public void checkAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        if (!"admin".equals(user.getRole())) {
            throw new BusinessException(403, "无权限，仅管理员可操作");
        }
    }
}
