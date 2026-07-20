package com.team6.moduply.watching.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team6.moduply.auth.JwtTokenProvider;
import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.auth.userdetails.ModuPlyUserDetailsService;
import com.team6.moduply.config.support.IntegrationTestSupport;
import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.enums.ContentType;
import com.team6.moduply.content.repository.ContentRepository;
import com.team6.moduply.common.enums.RedisKeyPolicy;
import com.team6.moduply.common.util.RedisUtil;
import com.team6.moduply.user.dto.UserCreateRequest;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.repository.UserRepository;
import com.team6.moduply.user.service.UserService;
import com.team6.moduply.watching.dto.WatchingSessionChange;
import com.team6.moduply.watching.enums.ChangeType;
import com.team6.moduply.watching.model.WatchingSession;
import com.team6.moduply.watching.repository.WatchingSessionRepository;
import java.lang.reflect.Type;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSession.Subscription;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
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
public abstract class WatchingSessionIntegrationTest extends IntegrationTestSupport {

  @LocalServerPort
  protected int port;

  private WebSocketStompClient stompClient;
  private String webSocketUrl;
  private String accessToken1;
  private String accessToken2;
  private UUID userId1;
  private UUID userId2;
  private UUID contentId;

  @Autowired
  private WatchingSessionRepository watchingSessionRepository;
  @Autowired
  private UserService userService;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private JwtTokenProvider jwtTokenProvider;
  @Autowired
  private RedisUtil redisUtil;
  @Autowired
  private ModuPlyUserDetailsService moduPlyUserDetailsService;
  @Autowired
  private ContentRepository contentRepository;

  private ThreadPoolTaskScheduler taskScheduler;

  @BeforeEach
  void setUp() {
    contentRepository.deleteAll();
    userRepository.deleteAll();

    // websocket 설정
    this.webSocketUrl = "http://localhost:" + port + "/ws";
    java.util.List<Transport> transports = java.util.List.of(
        new WebSocketTransport(new StandardWebSocketClient()));

    MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();

// 💡 역직렬화 트러블을 원천 차단하는 ObjectMapper 튜닝
    ObjectMapper customMapper = new ObjectMapper()
        .registerModule(
            new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule()) // LocalDateTime 지원
        .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
            false); // 없는 필드 무시

    converter.setObjectMapper(customMapper);

    SockJsClient sockJsClient = new SockJsClient(transports);
    this.stompClient = new WebSocketStompClient(sockJsClient);
    this.stompClient.setMessageConverter(converter);

    // 💡 비동기 클라이언트 이벤트를 정상 스케줄링하기 위한 태스크 스케줄러 추가
    taskScheduler =
        new ThreadPoolTaskScheduler();
    taskScheduler.initialize();
    this.stompClient.setTaskScheduler(taskScheduler);

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
    String sessionId1 = UUID.randomUUID().toString();
    accessToken1 = jwtTokenProvider.generateAccessToken(authentication, 0L, sessionId1);
    redisUtil.setDataExpire(
        RedisKeyPolicy.USER_TOKEN_VERSION.generateKey(testUser.getEmail()),
        "0",
        RedisKeyPolicy.USER_TOKEN_VERSION.getTtl()
    );
    redisUtil.setDataExpire(
        RedisKeyPolicy.AUTH_SESSION.generateKey(sessionId1),
        "ACTIVE",
        RedisKeyPolicy.AUTH_SESSION.getTtl()
    );
    userId1 = testUser.getId();

    // user 인증 정보 설정
    userService.createUser(
        new UserCreateRequest("test2", "test2@test.com", "test1234!"));
    User testUser2 = userRepository.findByEmail("test2@test.com").orElse(null);
    assertThat(testUser2).isNotNull();
    ModuPlyUserDetails moduPlyUserDetails2 = moduPlyUserDetailsService.loadUserByUsername(
        testUser2.getEmail());
    assertThat(moduPlyUserDetails2).isNotNull();
    Authentication authentication2 = new UsernamePasswordAuthenticationToken(moduPlyUserDetails2,
        moduPlyUserDetails2.getAuthorities());
    String sessionId2 = UUID.randomUUID().toString();
    accessToken2 = jwtTokenProvider.generateAccessToken(authentication2, 0L, sessionId2);
    redisUtil.setDataExpire(
        RedisKeyPolicy.USER_TOKEN_VERSION.generateKey(testUser2.getEmail()),
        "0",
        RedisKeyPolicy.USER_TOKEN_VERSION.getTtl()
    );
    redisUtil.setDataExpire(
        RedisKeyPolicy.AUTH_SESSION.generateKey(sessionId2),
        "ACTIVE",
        RedisKeyPolicy.AUTH_SESSION.getTtl()
    );
    userId2 = testUser2.getId();

    //Content
    Content content = new Content(null, null, ContentType.movie, "title", "description");
    contentRepository.save(content);
    contentId = content.getId();
  }

  @AfterEach
  void tearDown() {
    if (stompClient != null && stompClient.isRunning()) {
      stompClient.stop();
    }
    if (taskScheduler != null) {
      taskScheduler.destroy();
    }
  }

  @Test
  @DisplayName("웹소켓 SUBSCRIBE 요청이 성공하면 시청 세션 데이터가 정상 등록된다.")
  void creates_watching_session_success_when_subscribe() throws Exception {
    // given
    StompSession stompSession = connectStompSession(accessToken1);
    String destination = "/sub/contents/" + contentId + "/watch";

    // when
    stompSession.subscribe(destination, new EmptyFrameHandler());

    //then
    WatchingSession found = awaitSessionPresenceByUserId(userId1, true);
    assertThat(found).isNotNull();
    assertThat(found.getContentId()).isEqualTo(contentId);
    assertThat(found.getWatcher().userId()).isEqualTo(userId1);
    WatchingSession found2 = awaitSessionPresence(found.getSessionId(), true);
    assertThat(found).isEqualTo(found2);

    stompSession.disconnect();
  }

  @Test
  @DisplayName("웹소켓 UNSUBSCRIBE 요청이 정상 처리되면 시청 세션 데이터가 제거된다.")
  void removes_watchingSession_success_when_unsubscribe() throws Exception {
    //given
    StompSession stompSession = connectStompSession(accessToken1);
    String destination = "/sub/contents/" + contentId + "/watch";

    // 구독 먼저
    Subscription subscription = stompSession.subscribe(destination, new EmptyFrameHandler());
    WatchingSession found = awaitSessionPresenceByUserId(userId1, true);
    assertThat(found).isNotNull();

    assertThat(found).isNotNull();
    assertThat(found.getContentId()).isEqualTo(contentId);
    assertThat(found.getWatcher().userId()).isEqualTo(userId1);
    WatchingSession found2 = awaitSessionPresence(found.getSessionId(), true);
    assertThat(found).isEqualTo(found2);

    // when
    subscription.unsubscribe();

    // then
    WatchingSession foundAfterUnsubscribe = awaitSessionPresenceByUserId(userId1, false);
    assertThat(foundAfterUnsubscribe).isNull();
    WatchingSession found3 = awaitSessionPresence(found.getSessionId(), false);
    assertThat(found3).isNull();

    stompSession.disconnect();
  }

  @Test
  @DisplayName("웹소켓 세션 연결이 강제로 끊기면(DISCONNECT) 해당 연결 시청 세션이 삭제된다.")
  void removes_watching_session_when_disconnect() throws Exception {
    // given
    StompSession stompSession = connectStompSession(accessToken1);
    String destination = "/sub/contents/" + contentId + "/watch";

    // 구독
    stompSession.subscribe(destination, new EmptyFrameHandler());
    assertThat(awaitSessionPresenceByUserId(userId1, true)).isNotNull();

    // when
    stompSession.disconnect();

    // then
    WatchingSession foundAfterDisconnect = awaitSessionPresenceByUserId(userId1, false);
    assertThat(foundAfterDisconnect).isNull();
  }

  @Test
  @DisplayName("웹소켓 SUBSCRIBE 요청이 성공하면, 해당 방의 모든 유저에게 시청 세션 변경 알림 메시지가 발행된다.")
  void broadcasts_watching_session_change_message_success_when_subscribe() throws Exception {
    // given
    StompSession stompSession = connectStompSession(accessToken1);
    String destination = "/sub/contents/" + contentId + "/watch";

    CompletableFuture<WatchingSessionChange> messageExpectation = new CompletableFuture<>();

    // when
    // 💡 깔끔하게 주소만 넣고 구독 진행
    stompSession.subscribe(destination, new StompFrameHandler() {
      @Override
      public Type getPayloadType(StompHeaders headers) {
        return WatchingSessionChange.class;
      }

      @Override
      public void handleFrame(StompHeaders headers, Object payload) {
        messageExpectation.complete((WatchingSessionChange) payload);
      }
    });

    // then
    // 이제 컨버터 예외로 스레드가 뻗지 않으므로 정상적으로 메시지가 수신됩니다.
    WatchingSessionChange receivedMessage = messageExpectation.get(3, TimeUnit.SECONDS);

    assertThat(receivedMessage).isNotNull();
    assertThat(receivedMessage.type()).isEqualTo(ChangeType.JOIN);
    assertThat(receivedMessage.watchingSession().watcher().userId()).isEqualTo(userId1);
    assertThat(receivedMessage.watcherCount()).isEqualTo(1);
    stompSession.disconnect();
  }

  @Test
  @DisplayName("웹소켓 UNSUBSCRIBE 요청이 성공하면, 해당 방의 모든 유저에게 시청 세션 변경(LEAVE) 알림 메시지가 발행된다.")
  void broadcasts_watching_session_change_message_success_when_unsubscribe() throws Exception {
    // given
    String destination = "/sub/contents/" + contentId + "/watch";

    // 💡 1. 방에 남아서 이벤트를 관찰할 유저
    StompSession sessionB = connectStompSession(accessToken2);
    CompletableFuture<WatchingSessionChange> messageExpectation = new CompletableFuture<>();

    sessionB.subscribe(destination, new StompFrameHandler() {
      @Override
      public Type getPayloadType(StompHeaders headers) {
        return WatchingSessionChange.class;
      }

      @Override
      public void handleFrame(StompHeaders headers, Object payload) {
        WatchingSessionChange message = (WatchingSessionChange) payload;
        if (message.type() == ChangeType.LEAVE) {
          messageExpectation.complete(message);
        }
      }
    });
    assertThat(awaitSessionPresenceByUserId(userId2, true)).isNotNull();

    // 💡 2. 방을 나갈 유저 A(본인) 연결 및 구독
    StompSession sessionA = connectStompSession(accessToken1); // 기존 기존 유저(testUser)
    Subscription subscriptionA = sessionA.subscribe(destination, new EmptyFrameHandler());
    assertThat(awaitSessionPresenceByUserId(userId1, true)).isNotNull();

    // when
    // 3. 유저 A가 구독을 해제합니다.
    subscriptionA.unsubscribe();

    // then
    // 4. 방에 남아있던 유저 B에게 유저 A가 나갔다는 LEAVE 메시지가 정상 도달합니다.
    WatchingSessionChange receivedMessage = messageExpectation.get(3, TimeUnit.SECONDS);

    // ✨ 이제 watcherCount는 1이 되어야 정상입니다! (방에 유저 B가 남아있으므로)
    assertThat(receivedMessage).isNotNull();
    assertThat(receivedMessage.type()).isEqualTo(ChangeType.LEAVE);
    assertThat(receivedMessage.watchingSession().watcher().userId()).isEqualTo(
        userId1); // 나간 사람은 A이다
    assertThat(receivedMessage.watcherCount()).isEqualTo(1); // 남은 사람은 B 혼자이므로 1명!

    sessionA.disconnect();
    sessionB.disconnect();
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
  private StompSession connectStompSession(String accessToken) throws Exception {
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
