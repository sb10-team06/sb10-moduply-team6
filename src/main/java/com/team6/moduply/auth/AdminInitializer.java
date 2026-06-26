package com.team6.moduply.auth;

import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.enums.Role;
import com.team6.moduply.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer {
  @Value("${admin.init.email}")
  private String adminEmail;

  @Value("${admin.init.password}")
  private String adminPassword;

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  public void initAdmin() {
  //
    if (userRepository.existsByRole(Role.ADMIN)) {
      log.info("Admin 계졍이 이미 존재합니다.");
      return;
    }

    userRepository.save(new User(
        adminEmail,
        passwordEncoder.encode(adminPassword),
        "Admin",
        Role.ADMIN));
    log.info("초기 ADMIN 계정이 생성되었습니다! [Email = {}]", adminEmail);
  }
}
