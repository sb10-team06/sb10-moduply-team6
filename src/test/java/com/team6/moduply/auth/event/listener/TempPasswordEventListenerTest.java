package com.team6.moduply.auth.event.listener;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.team6.moduply.auth.event.TempPasswordEvent;
import com.team6.moduply.common.enums.RedisKeyPolicy;
import com.team6.moduply.common.util.RedisUtil;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class TempPasswordEventListenerTest {

  @Mock
  private JavaMailSender mailSender;

  @Mock
  private RedisUtil redisUtil;

  @Mock
  private PasswordEncoder passwordEncoder;

  @InjectMocks
  private EmailEventListener emailEventListener;

  @Test
  @DisplayName("임시 비밀번호 메일 발송 성공 후 Redis에 인코딩된 임시 비밀번호를 3분 TTL로 저장한다")
  void handle_password_reset_event_success() {
    // Given
    String email = "tester@example.com";
    String tempPassword = "Temp1234!";
    String encodedTempPassword = "encoded-temp-password";
    TempPasswordEvent event = new TempPasswordEvent(
        email,
        tempPassword,
        Instant.parse("2026-06-29T02:00:00Z")
    );
    MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));

    given(mailSender.createMimeMessage()).willReturn(mimeMessage);
    given(passwordEncoder.encode(tempPassword)).willReturn(encodedTempPassword);

    // When
    emailEventListener.handlePasswordResetEvent(event);

    // Then
    verify(mailSender).send(mimeMessage);
    verify(redisUtil).setDateExpire(
        RedisKeyPolicy.PASSWORD_RESET.generateKey(email),
        encodedTempPassword,
        RedisKeyPolicy.PASSWORD_RESET.getTtl()
    );
  }
}
