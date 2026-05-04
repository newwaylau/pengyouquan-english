package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户数据访问层
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** 按邮箱查找用户 */
    Optional<User> findByEmail(String email);

    /** 检查邮箱是否已注册 */
    boolean existsByEmail(String email);

    /** 按微信 openId 查找用户 */
    Optional<User> findByWechatOpenId(String wechatOpenId);
}
