package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户设置数据访问层
 */
@Repository
public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {

    /** 查某个用户的所有设置 */
    java.util.List<UserSetting> findByUserId(Long userId);

    /** 查某个用户的单个设置 */
    java.util.Optional<UserSetting> findByUserIdAndSettingKey(Long userId, String settingKey);

    /** 删除某个用户的单个设置 */
    @Transactional
    void deleteByUserIdAndSettingKey(Long userId, String settingKey);

    /** 把设置列表转成 Map */
    default java.util.Map<String, String> getSettingsMap(Long userId) {
        return findByUserId(userId).stream()
                .collect(Collectors.toMap(UserSetting::getSettingKey, UserSetting::getSettingValue));
    }
}
