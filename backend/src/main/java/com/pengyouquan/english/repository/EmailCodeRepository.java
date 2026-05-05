package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.EmailCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 邮箱验证码数据访问层
 */
@Repository
public interface EmailCodeRepository extends JpaRepository<EmailCode, Long> {

    /** 按邮箱查找最新未使用的验证码 */
    Optional<EmailCode> findTopByEmailOrderByCreatedAtDesc(String email);

    /** 按邮箱和验证码查找 */
    Optional<EmailCode> findByEmailAndCode(String email, String code);

    /** 删除某邮箱的所有验证码 */
    void deleteByEmail(String email);

    /** 删除创建时间早于指定时间的验证码（清理过期） */
    void deleteByCreatedAtBefore(LocalDateTime time);
}
