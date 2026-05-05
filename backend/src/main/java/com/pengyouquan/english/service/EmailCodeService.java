package com.pengyouquan.english.service;

import com.pengyouquan.english.model.EmailCode;
import com.pengyouquan.english.repository.EmailCodeRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class EmailCodeService {

    private final EmailCodeRepository emailCodeRepository;
    private static final SecureRandom RANDOM = new SecureRandom();

    public EmailCodeService(EmailCodeRepository emailCodeRepository) {
        this.emailCodeRepository = emailCodeRepository;
    }

    public String sendCode(String email) {
        String code = String.format("%06d", RANDOM.nextInt(999999));
        EmailCode ec = new EmailCode();
        ec.setEmail(email);
        ec.setCode(code);
        emailCodeRepository.save(ec);
        return code;
    }

    public String generateAndSaveCode(String email) {
        return sendCode(email);
    }

    public boolean verifyCode(String email, String code) {
        return emailCodeRepository.findByEmailAndCode(email, code).isPresent();
    }
}
