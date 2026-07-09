package com.team6.moduply.common.websocket.listener;

import com.team6.moduply.auth.event.UserLogoutEvent;
import com.team6.moduply.common.websocket.service.WebsocketControlService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebSocketLogoutDisconnectListenerTest {

  @Mock
  private WebsocketControlService websocketControlService;

  @InjectMocks
  private WebSocketLogoutDisconnectListener eventListener;

  @Test
  @DisplayName("로그아웃 이벤트가 발생하면 서비스의 강제 종료 신호가 호출되어야 한다")
  void call_success_when_logout_event_occurred() {
    // given
    UUID userId = UUID.randomUUID();
    String email = "test@moduply.com";
    UserLogoutEvent event = new UserLogoutEvent(userId, email);

    // 2. when
    eventListener.on(event);

    // then
    verify(websocketControlService, times(1)).sendForceLogoutSignal(email);
  }

  @Test
  @DisplayName("서비스에서 예외 발생 시 예외가 전파되지 않아야 한다")
  void should_not_propagate_exception_when_service_fails() {
    // given
    UUID userId = UUID.randomUUID();
    String email = "test@moduply.com";
    UserLogoutEvent event = new UserLogoutEvent(userId, email);
    doThrow(new RuntimeException("send failed"))
        .when(websocketControlService).sendForceLogoutSignal(email);

    // when & then
    assertThatCode(() -> eventListener.on(event))
        .doesNotThrowAnyException();
    verify(websocketControlService, times(1)).sendForceLogoutSignal(email);
  }
}