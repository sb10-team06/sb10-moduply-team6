package com.team6.moduply.auth.event.listener;

import com.team6.moduply.auth.event.EmailEvent;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class EmailEventListener {
  private final JavaMailSender mailSender;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handlePasswordResetEvent(EmailEvent event) {
    log.info("[비동기 메일 발송 시작] 수신자: {}", event.getEmail());

    try {
      // HTML 메일을 보내기 위해 MimeMessage를 생성
      MimeMessage mimeMessage = mailSender.createMimeMessage();

      // MimeMessageHelper를 사용하면 멀티파트 및 인코딩 설정을 편하게 할 수 있음.
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

      // 현재 시간 기준으로 정확히 3분 뒤의 만료 시간을 포맷에 맞게 계산
      String expiredTime = LocalDateTime.now().plusMinutes(3)
          .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

      helper.setTo(event.getEmail());
      helper.setSubject("[모두의 플리] 요청하신 임시 비밀번호가 발급되었습니다.");

      // 🎨 보내주신 요구사항 디자인을 그대로 HTML/CSS로 재현한 템플릿입니다.
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
      log.info("[비동기 메일 발송 성공] 수신자: {}", event.getEmail());

    } catch (Exception e) {
      log.error("[비동기 메일 발송 실패] 수신자: {}, 원인: {}", event.getEmail(), e.getMessage());
    }
  }

}
