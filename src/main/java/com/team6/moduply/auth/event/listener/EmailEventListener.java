package com.team6.moduply.auth.event.listener;

import com.team6.moduply.auth.event.TempPasswordEvent;
import com.team6.moduply.common.config.AsyncConfig;
import com.team6.moduply.common.enums.RedisKeyPolicy;
import com.team6.moduply.common.util.RedisUtil;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class EmailEventListener {
  private final JavaMailSender mailSender;
  private final RedisUtil redisUtil;
  private final PasswordEncoder passwordEncoder;

  @Async(AsyncConfig.TEMP_PASSWORD_MAIL_TASK_EXECUTOR)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handlePasswordResetEvent(TempPasswordEvent event) {
    // TODO: @Async + AFTER_COMMIT 이벤트는 재시작/배포 시 유실될 수 있으므로 실패 전파 정책과
    //  outbox 또는 내구성 있는 큐 기반 처리로 전환하는 방안을 검토한다.
    String email = event.getEmail();
    log.info("[비동기 메일 발송 시작] 수신자: {}", email);
    String requestId = MDC.get("requestId");
    try {
      // HTML 메일을 보내기 위해 MimeMessage를 생성
      MimeMessage mimeMessage = mailSender.createMimeMessage();

      // MimeMessageHelper를 사용하면 멀티파트 및 인코딩 설정을 편하게 할 수 있음.
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

      // redis의 키값과 만료시각, 인코딩된 비밀번호
      String redisKey = RedisKeyPolicy.PASSWORD_RESET.generateKey(email);
      Duration ttl = RedisKeyPolicy.PASSWORD_RESET.getTtl();
      String encodedTempPassword = passwordEncoder.encode(event.getTempPassword());

      // 발급 요청 시점 기준 만료 시간을 메일 표시 형식으로 변환한다.
      String expiredTime = DateTimeFormatter
          .ofPattern("yyyy-MM-dd HH:mm:ss")
          .withZone(ZoneId.of("Asia/Seoul"))
          .format(event.getExpiresAt());

      helper.setTo(email);
      helper.setSubject("[모두의 플리] 요청하신 임시 비밀번호가 발급되었습니다.");

      // 🎨 보내주신 요구사항 디자인을 그대로 HTML/CSS로 재현한 템플릿입니다.
      // TODO: html/css 파일로 따로 작성하여 관리하는것 고려해보기
      String htmlContent = """
                <div style="font-family: 'Malgun Gothic', 'Apple SD Gothic Neo', sans-serif; max-width: 550px; margin: 0 auto; padding: 25px; border: 1px solid #E2E8F0; border-radius: 8px;">
                    
                    <h2 style="color: #1A202C; margin-bottom: 20px; font-size: 22px; font-weight: bold;">모두의 플리</h2>
                    
                    <h3 style="color: #2D3748; margin-bottom: 15px; font-size: 18px;">임시 비밀번호가 발급되었습니다</h3>
                    
                    <p style="color: #4A5568; line-height: 1.6; margin-bottom: 5px;">안녕하세요!</p>
                    <p style="color: #4A5568; line-height: 1.6; margin-bottom: 20px;">
                        요청하신 임시 비밀번호가 발급되었습니다. 아래 임시 비밀번호를 사용하여 로그인 후 새로운 비밀번호로 변경해주세요.
                    </p>
                    
                    <div style="background-color: #F7FAFC; padding: 15px 20px; border-left: 4px solid #3182CE; margin-bottom: 25px;">
                        <div style="font-size: 14px; color: #718096; margin-bottom: 4px;">임시 비밀번호</div>
                        <div style="font-size: 20px; font-weight: bold; color: #2B6CB0; letter-spacing: 0.5px;">%s</div>
                    </div>
                    
                    <div style="background-color: #FFF5F5; padding: 15px 20px; border-radius: 6px; margin-bottom: 30px;">
                        <p style="margin-top: 0; margin-bottom: 8px; font-weight: bold; color: #E53E3E; font-size: 15px;">⚠️ 중요 안내사항</p>
                        <ul style="margin: 0; padding-left: 20px; color: #4A5568; line-height: 1.8; font-size: 14px;">
                            <li>이 임시 비밀번호는 <strong style="color: #E53E3E;">%s</strong>까지만 유효합니다</li>
                            <li>보안을 위해 로그인 후 즉시 새로운 비밀번호로 변경해주세요</li>
                            <li>임시 비밀번호는 다른 사람과 공유하지 마세요</li>
                        </ul>
                    </div>
                    
                    <hr style="border: 0; border-top: 1px solid #EDF2F7; margin-bottom: 15px;">
                    
                    <p style="font-size: 12px; color: #A0AEC0; line-height: 1.6; margin: 0;">
                        본 메일은 발신전용이므로 회신되지 않습니다.<br>
                        문의사항이 있으시면 고객센터로 연락해주세요.
                    </p>
                    
                </div>
                """.formatted(event.getTempPassword(), expiredTime);

      // 두 번째 인자에 true를 넘겨야 단순 텍스트가 아닌 HTML로 이메일이 발송됨
      helper.setText(htmlContent, true);

      mailSender.send(mimeMessage);

      // 이메일 발송 후 redis에 인코딩된 임시비밀번호, 만료시간 저장
      redisUtil.setDateExpire(redisKey, encodedTempPassword, ttl);

      log.info("[비동기 메일 발송 성공] 요청 Id = {}", requestId);

    } catch (Exception e) {
      log.error("[비동기 메일 발송 실패] 요청 Id = {}, 원인: {}", requestId, e.getMessage());
    }
  }

}
