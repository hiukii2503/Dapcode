package com.dapcode.backend_core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.dapcode.backend_core.entity.Users;
import com.dapcode.backend_core.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:dapcode_admin}")
    private String adminUsername;

    @Value("${app.admin.email:admin@dapcode.io}")
    private String adminEmail;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Users admin = userRepository.findByEmail(adminEmail)
                .or(() -> userRepository.findByUsername(adminUsername))
                .orElse(null);

        if (admin == null) {
            log.info("[AdminInitializer] Admin account not found. Initializing root admin: {}", adminEmail);
            admin = Users.builder()
                    .username(adminUsername)
                    .email(adminEmail)
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .role("ROLE_ADMIN")
                    .avatarUrl("https://api.dicebear.com/7.x/bottts/svg?seed=" + adminUsername)
                    .xp(9999)
                    .streakCount(30)
                    .isActive(true)
                    .build();
            userRepository.save(admin);
            log.info("[AdminInitializer] Root admin created successfully with BCrypt hashed password.");
        } else {
            boolean updated = false;

            if (admin.getPasswordHash() == null || admin.getPasswordHash().isBlank()) {
                log.info("[AdminInitializer] Admin password hash is empty. Hashing and updating password...");
                admin.setPasswordHash(passwordEncoder.encode(adminPassword));
                updated = true;
            }

            if (!"ROLE_ADMIN".equalsIgnoreCase(admin.getRole())) {
                admin.setRole("ROLE_ADMIN");
                updated = true;
            }

            if (updated) {
                userRepository.save(admin);
                log.info("[AdminInitializer] Admin account updated successfully.");
            } else {
                log.info("[AdminInitializer] Admin account is already configured: {}", admin.getEmail());
            }
        }
    }
}

