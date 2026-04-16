package com.shepherdsstories.services;

public interface EmailService {
    void sendVerificationEmail(String to, String token);
}
