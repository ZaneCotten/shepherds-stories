package com.shepherdsstories.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ConsoleEmailService implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleEmailService.class);

    @Override
    public void sendVerificationEmail(String to, String token) {
        String verificationUrl = "http://localhost:5173/verify-email?token=" + token;
        logger.info("Sending verification email to: {}", to);
        logger.info("Verification URL: {}", verificationUrl);
    }
}
