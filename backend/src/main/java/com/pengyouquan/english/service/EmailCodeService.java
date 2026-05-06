package com.pengyouquan.english.service;

import com.pengyouquan.english.model.EmailCode;
import com.pengyouquan.english.repository.EmailCodeRepository;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.pengyouquan.english.config.GlobalExceptionHandler.BusinessException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 邮箱验证码服务
 */
@Service
public class EmailCodeService {

    private final EmailCodeRepository emailCodeRepository;
    private final JavaMailSender mailSender;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String FROM = "newwaylau@163.com";

    public EmailCodeService(EmailCodeRepository emailCodeRepository, JavaMailSender mailSender) {
        this.emailCodeRepository = emailCodeRepository;
        this.mailSender = mailSender;
    }

    /** 生成并发送验证码 */
    public String sendCode(String email) {
        String code = String.format("%06d", RANDOM.nextInt(999999));
        EmailCode ec = new EmailCode();
        ec.setEmail(email);
        ec.setCode(code);
        emailCodeRepository.save(ec);

        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(FROM);
            helper.setTo(email);
            helper.setSubject("朋友圈英语 - 验证码");
            helper.setText("您的验证码为：<b>" + code + "</b><br>有效期5分钟，如非本人操作请忽略。", true);
            mailSender.send(msg);
        } catch (Exception e) {
            // 发送失败不阻塞注册
            e.printStackTrace();
        }
        return code;
    }

    public String generateAndSaveCode(String email) {
        return sendCode(email);
    }

    /**
     * 校验邮箱验证码（检查存在性 + 5分钟有效期），校验成功后删除已使用
     */
    public void verifyCode(String email, String code) {
        Optional<EmailCode> record = emailCodeRepository.findByEmailAndCode(email, code);
        if (record.isEmpty()) {
            throw new BusinessException("验证码错误");
        }

        // 检查5分钟有效期
        EmailCode ec = record.get();
        if (ec.getCreatedAt().plusMinutes(5).isBefore(java.time.LocalDateTime.now())) {
            emailCodeRepository.delete(ec);
            throw new BusinessException("验证码已过期，请重新获取");
        }

        // 校验通过后删除已使用验证码
        emailCodeRepository.delete(ec);
    }
}
