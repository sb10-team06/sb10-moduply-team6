package com.team6.moduply.common.websocket.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;

@ExtendWith(MockitoExtension.class)
class WebsocketControlServiceTest {

  @Mock
  private MessageChannel clientOutboundChannel;
  @Mock
  private SimpUserRegistry userRegistry;
  @InjectMocks
  private WebsocketControlService websocketControlService;

  @Test
  @DisplayName("에러 프레임이 정상적으로 전송되어야 한다")
  void send_force_logout_signal_success() {
    // given
    String email = "test@test.com";
    SimpUser mockUser = mock(SimpUser.class);
    SimpSession mockSession = mock(SimpSession.class);

    when(userRegistry.getUser(email)).thenReturn(mockUser);
    when(mockUser.getSessions()).thenReturn(Set.of(mockSession));
    when(mockSession.getId()).thenReturn("session-123");

    // when
    websocketControlService.sendForceLogoutSignal(email);

    // then
    verify(clientOutboundChannel).send(any(Message.class));
  }

  @Test
  @DisplayName("사용자를 찾을 수 없으면 전송하지 않는다")
  void send_force_logout_signal_user_not_found() {
    // given
    String email = "unknown@test.com";
    when(userRegistry.getUser(email)).thenReturn(null);

    // when
    websocketControlService.sendForceLogoutSignal(email);

    // then
    verify(clientOutboundChannel, never()).send(any(Message.class));
  }

  @Test
  @DisplayName("다중 세션에 대해 각각 전송한다")
  void send_force_logout_signal_multiple_sessions() {
    // given
    String email = "test@test.com";
    SimpUser mockUser = mock(SimpUser.class);
    SimpSession session1 = mock(SimpSession.class);
    SimpSession session2 = mock(SimpSession.class);
    when(userRegistry.getUser(email)).thenReturn(mockUser);
    when(mockUser.getSessions()).thenReturn(Set.of(session1, session2));
    when(session1.getId()).thenReturn("s1");
    when(session2.getId()).thenReturn("s2");

    // when
    websocketControlService.sendForceLogoutSignal(email);

    // then
    verify(clientOutboundChannel, times(2)).send(any(Message.class));
  }
}
