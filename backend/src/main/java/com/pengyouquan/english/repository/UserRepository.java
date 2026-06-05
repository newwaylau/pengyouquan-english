package com.pengyouquan.english.repository;

import com.pengyouquan.english.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问层
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** 按邮箱查找用户 */
    Optional<User> findByEmail(String email);

    /** 按手机号查找用户 */
    Optional<User> findByPhone(String phone);

    /** 检查邮箱是否已注册 */
    boolean existsByEmail(String email);

    /** 按微信 openId 查找用户 */
    Optional<User> findByWechatOpenId(String wechatOpenId);

    /** 按角色查找（分页，管理员用） */
    Page<User> findByRole(String role, Pageable pageable);

    /** 搜索用户（按昵称或邮箱模糊匹配） */
    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.nickname LIKE %:q% OR u.email LIKE %:q%")
    List<User> searchByKeyword(@org.springframework.data.repository.query.Param("q") String q);
}
