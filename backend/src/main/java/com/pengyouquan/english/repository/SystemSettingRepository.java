package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 系统设置数据访问层
 */
@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {
}
