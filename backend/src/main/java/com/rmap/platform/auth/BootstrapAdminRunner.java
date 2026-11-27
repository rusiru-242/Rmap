package com.rmap.platform.auth;

import com.rmap.platform.billing.BillingService;
import com.rmap.platform.config.RmapProperties;
import com.rmap.platform.user.Role;
import com.rmap.platform.user.User;
import com.rmap.platform.user.UserRepository;
import com.rmap.platform.user.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class BootstrapAdminRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);

    private final RmapProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BillingService billingService;

    public BootstrapAdminRunner(
            RmapProperties properties,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            BillingService billingService
    ) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.billingService = billingService;
    }

    @Override
    public void run(String... args) {
        String email = properties.bootstrap().adminEmail();
        String password = properties.bootstrap().adminPassword();
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return;
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalized)) {
            return;
        }
        User admin = new User(
                normalized,
                passwordEncoder.encode(password),
                "Rmap Administrator",
                Role.ADMIN,
                UserStatus.ACTIVE,
                true
        );
        admin = userRepository.save(admin);
        billingService.provisionTrialIfMissing(admin.getId());
        log.info("Bootstrapped local admin account for {}", normalized);
    }
}
