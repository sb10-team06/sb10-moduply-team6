package com.team6.moduply.watching.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.team6.moduply.auth.JwtTokenProvider;
import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.auth.userdetails.ModuPlyUserDetailsService;
import com.team6.moduply.config.support.IntegrationTestSupport;
import com.team6.moduply.user.dto.UserCreateRequest;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.repository.UserRepository;
import com.team6.moduply.user.service.UserService;
import com.team6.moduply.watching.model.WatchingSession;
import com.team6.moduply.watching.repository.WatchingSessionRepository;
import java.lang.reflect.Type;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSession.Subscription;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;

import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WatchingSessionIntegrationTest extends IntegrationTestSupport {

  @LocalServerPort
  private int port;

  private WebSocketStompClient stompClient;
  private String webSocketUrl;
  private String accessToken;
  private UUID userId;

  @Autowired
  private WatchingSessionRepository watchingSessionRepository;
  @Autowired
  private UserService userService;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private JwtTokenProvider jwtTokenProvider;
  @Autowired
  private ModuPlyUserDetailsService moduPlyUserDetailsService;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();

    // websocket 설정
    this.webSocketUrl = "http://localhost:" + port + "/ws";
    java.util.List<Transport> transports = java.util.List.of(
        new WebSocketTransport(new StandardWebSocketClient()));
    SockJsClient sockJsClient = new SockJsClient(transports);
    this.stompClient = new WebSocketStompClient(sockJsClient);

    // user 설정
    // user 인증 정보 설정
    userService.createUser(
        new UserCreateRequest("test", "test@test.com", "test1234!"));
    User testUser = userRepository.findByEmail("test@test.com").orElse(null);
    assertThat(testUser).isNotNull();
    ModuPlyUserDetails moduPlyUserDetails = moduPlyUserDetailsService.loadUserByUsername(
        testUser.getEmail());
    assertThat(moduPlyUserDetails).isNotNull();
    Authentication authentication = new UsernamePasswordAuthenticationToken(moduPlyUserDetails,
        moduPlyUserDetails.getAuthorities());
    accessToken = jwtTokenProvider.generateAccessToken(authentication);
    userId = testUser.getId();
  }

  @Test
  @DisplayName("웹소켓 SUBSCRIBE 요청이 성공하면 시청 세션 데이터가 정상 등록된다.")
  void creates_watching_session_success_when_subscribe() throws Exception {
    // given
    UUID contentId = UUID.randomUUID();
    StompSession stompSession = connectStompSession();
    String destination = "/sub/contents/" + contentId + "/watch";

    // when
    stompSession.subscribe(destination, new EmptyFrameHandler());

    //then
    WatchingSession found = awaitSessionPresenceByUserId(userId, true);
    assertThat(found).isNotNull();
    assertThat(found.getContentId()).isEqualTo(contentId);
    assertThat(found.getWatcherId()).isEqualTo(userId);
    WatchingSession found2 = awaitSessionPresence(found.getSessionId(), true);
    assertThat(found).isEqualTo(found2);

    stompSession.disconnect();
  }

  @Test
  @DisplayName("웹소켓 UNSUBSCRIBE 요청이 정상 처리되면 시청 세션 데이터가 제거된다.")
  void removes_watchingSession_success_when_unsubscribe() throws Exception {
    //given
    UUID contentId = UUID.randomUUID();
    StompSession stompSession = connectStompSession();
    String destination = "/sub/contents/" + contentId + "/watch";

    // 구독 먼저
    Subscription subscription = stompSession.subscribe(destination, new EmptyFrameHandler());
    WatchingSession found = awaitSessionPresenceByUserId(userId, true);
    assertThat(found).isNotNull();

    assertThat(found).isNotNull();
    assertThat(found.getContentId()).isEqualTo(contentId);
    assertThat(found.getWatcherId()).isEqualTo(userId);
    WatchingSession found2 = awaitSessionPresence(found.getSessionId(), true);
    assertThat(found).isEqualTo(found2);

    // when
    subscription.unsubscribe();

    // then
    WatchingSession foundAfterUnsubscribe = awaitSessionPresenceByUserId(userId, false);
    assertThat(foundAfterUnsubscribe).isNull();
    WatchingSession found3 = awaitSessionPresence(found.getSessionId(), false);
    assertThat(found3).isNull();

    stompSession.disconnect();
  }

  @Test
  @DisplayName("웹소켓 세션 연결이 강제로 끊기면(DISCONNECT) 해당 연결 시청 세션이 삭제된다.")
  void removes_watching_session_when_disconnect() throws Exception {
    // given
    UUID contentId = UUID.randomUUID();
    StompSession stompSession = connectStompSession();
    String destination = "/sub/contents/" + contentId + "/watch";

    // 구독
    stompSession.subscribe(destination, new EmptyFrameHandler());
    assertThat(awaitSessionPresenceByUserId(userId, true)).isNotNull();

    // when
    stompSession.disconnect();

    // then
    WatchingSession foundAfterDisconnect = awaitSessionPresenceByUserId(userId, false);
    assertThat(foundAfterDisconnect).isNull();
  }

  // 저장소에서 사용자 아이디로 조회
  private WatchingSession awaitSessionPresenceByUserId(UUID userId, boolean expectVisible)
      throws InterruptedException {
    long deadline = System.currentTimeMillis() + 3000;
    while (System.currentTimeMillis() < deadline) {
      var opt = watchingSessionRepository.findByUserId(userId);
      if (opt.isPresent() == expectVisible) {
        return opt.orElse(null);
      }
      Thread.sleep(100);
    }
    return watchingSessionRepository.findByUserId(userId).orElse(null);
  }

  // 웹소켓 연결 세션 설정
  private StompSession connectStompSession() throws Exception {
    CompletableFuture<StompSession> connectFuture = new CompletableFuture<>();

    // Bearer 헤더를 주입
    StompHeaders stompHeaders = new StompHeaders();
    stompHeaders.add("Origin", "http://localhost");
    stompHeaders.add("Authorization", "Bearer " + accessToken);

    WebSocketHttpHeaders webSocketHttpHeaders = new WebSocketHttpHeaders();
    webSocketHttpHeaders.add("Origin", "http://localhost");

    stompClient.connectAsync(
        webSocketUrl,
        webSocketHttpHeaders,
        stompHeaders,
        new StompSessionHandlerAdapter() {
          @Override
          public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            connectFuture.complete(session);
          }
        }
    );
    return connectFuture.get(3, TimeUnit.SECONDS);
  }

  // 세션 아이디로 조회
  private WatchingSession awaitSessionPresence(String sessionId, boolean expectVisible)
      throws InterruptedException {
    long deadline = System.currentTimeMillis() + 3000;
    while (System.currentTimeMillis() < deadline) {
      var opt = watchingSessionRepository.findBySessionId(sessionId);
      if (opt.isPresent() == expectVisible) {
        return opt.orElse(null);
      }
      Thread.sleep(100);
    }
    return watchingSessionRepository.findBySessionId(sessionId).orElse(null);
  }

  private static class EmptyFrameHandler implements StompFrameHandler {

    @Override
    public Type getPayloadType(StompHeaders headers) {
      return byte[].class;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
    }
  }
}