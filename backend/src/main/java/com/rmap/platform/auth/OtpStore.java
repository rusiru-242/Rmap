package com.rmap.platform.auth;

public interface OtpStore {
    String generateAndStore(String email, String purpose);
    boolean verifyAndConsume(String email, String purpose, String code);
}
