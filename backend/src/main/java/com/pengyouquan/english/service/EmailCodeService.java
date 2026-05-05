package com.pengyouquan.english.service;

import com.pengyouquan.english.config.GlobalExceptionHandler.BusinessException;
import com.pengyouquan.english.model.EmailCode;
import com.pengyouquan.english.repository.EmailCodeRepository;
import com.pengyouquan.english.repository.UserRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * 邮箱验证码服务
 * 生成验证码、发送邮件、校验验证码
 */
@Service
public class EmailCodeService {

    private final EmailCodeRepository emailCodeRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final Random random = new Random();

    public EmailCodeService(EmailCodeRepository emailCodeRepository,
                            UserRepository userRepository,
                            JavaMailSender mailSender) {
        this.emailCodeRepository = emailCodeRepository;
        this.userRepository = userRepository;
        this.mailSender = mailSender;
    }

    /**
     * 生成并发送验证码
     */
    public void sendCode(String email) {
        // 检查是否已注册
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("该邮箱已注册");
        }

        // 生成6位随机验证码
        String code = String.format("%06d", random.nextInt(1000000));

        // 保存到数据库
        EmailCode emailCode = new EmailCode();
        emailCode.setEmail(email);
        emailCode.setCode(code);
        emailCodeRepository.save(emailCode);

        // 发送邮件
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("朋友圈英语 - 邮箱验证码");
        message.setText("您的验证码是：" + code + "，有效期10分钟。如非本人操作，请忽略。");
        mailSender.send(message);
    }

    /**
     * 验证验证码是否正确（校验后删除已使用的验证码）
     */
    public void verifyCode(String email, String code) {
        EmailCode record = emailCodeRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new BusinessException("验证码未发送或已失效"));

        // 检查是否过期（10分钟）
        if (record.getCreatedAt().plusMinutes(10).isBefore(LocalDateTime.now())) {
            emailCodeRepository.delete(record);
            throw new BusinessException("验证码已过期，请重新发送");
        }

        // 检查验证码是否匹配
        if (!record.getCode().equals(code)) {
            throw new BusinessException("验证码错误");
        }

        // 删除已使用的验证码
        emailCodeRepository.deleteByEmail(email);
    }
}
