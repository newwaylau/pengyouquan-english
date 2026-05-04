package com.pengyouquan.english.service;

import com.pengyouquan.english.model.UserSetting;
import com.pengyouquan.english.repository.UserSettingRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 用户设置服务
 */
@Service
public class SettingsService {

    private final UserSettingRepository settingRepository;

    public SettingsService(UserSettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    /** 获取全部设置 */
    public Map<String, String> getAllSettings(Long userId) {
        return settingRepository.getSettingsMap(userId);
    }

    /** 批量保存设置（已有则更新，没有则新建） */
    public void saveSettings(Long userId, Map<String, String> settings) {
        settings.forEach((key, value) -> {
            UserSetting setting = settingRepository
                    .findByUserIdAndSettingKey(userId, key)
                    .orElseGet(() -> {
                        UserSetting s = new UserSetting();
                        s.setUserId(userId);
                        s.setSettingKey(key);
                        return s;
                    });
            setting.setSettingValue(value);
            settingRepository.save(setting);
        });
    }

    /** 获取单个设置 */
    public String getSetting(Long userId, String key) {
        return settingRepository.findByUserIdAndSettingKey(userId, key)
                .map(UserSetting::getSettingValue)
                .orElse(null);
    }

    /** 删除单个设置 */
    public void deleteSetting(Long userId, String key) {
        settingRepository.deleteByUserIdAndSettingKey(userId, key);
    }
}
