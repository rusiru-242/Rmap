package com.rmap.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "rmap")
public record RmapProperties(
        SecurityProperties security,
        MailProperties mail,
        BillingProperties billing,
        BootstrapProperties bootstrap
) {
    public record SecurityProperties(
            String jwtSecret,
            long accessTokenTtlSeconds,
            long refreshTokenTtlSeconds,
            long otpTtlSeconds,
            List<String> allowedOrigins
    ) {}

    public record MailProperties(
            String from
    ) {}

    public record BillingProperties(
            int trialHours,
            String provider,
            String webhookSecret
    ) {}

    public record BootstrapProperties(
            String adminEmail,
            String adminPassword
    ) {}
}
