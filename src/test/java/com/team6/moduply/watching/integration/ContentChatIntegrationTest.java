package com.team6.moduply.watching.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team6.moduply.auth.JwtTokenProvider;
import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.auth.userdetails.ModuPlyUserDetailsService;
import com.team6.moduply.config.support.IntegrationTestSupport;
import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.enums.ContentType;
import com.team6.moduply.content.repository.ContentRepository;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.enums.Role;
import com.team6.moduply.user.repository.UserRepository;
import com.team6.moduply.watching.dto.ContentChatDto;
import com.team6.moduply.watching.dto.ContentChatSendRequest;
import com.team6.moduply.watching.model.WatchingSession;
import com.team6.moduply.watching.repository.WatchingSessionRepository;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ContentChatIntegrationTest extends IntegrationTestSupport {

  @LocalServerPort
  private int port;

  @Autowired
  private UserRepository userRepository;
  @Autowired
  private ContentRepository contentRepository;
  @Autowired
  private ModuPlyUserDetailsService moduPlyUserDetailsService;
  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  private UUID user1Id;
  private UUID user2Id;
  private UUID user3Id;
  private User user1;
  private String accessToken1;
  private String accessToken2;
  private String accessToken3;

  private UUID content1Id;
  private UUID content2Id;

  private StompSession stompSession1;
  private StompSession stompSession2;
  private StompSession stompSession3;
  private String sessionId3;

  private WebSocketStompClient stompClient;
  private String webSocketUrl;
  private ThreadPoolTaskScheduler taskScheduler;
  @Autowired
  private WatchingSessionRepository watchingSessionRepository;

  @BeforeEach
  void setUp() throws Exception {
    contentRepository.deleteAll();
    userRepository.deleteAll();

    // websocket 설정
    this.webSocketUrl = "http://localhost:" + port + "/ws";
    java.util.List<Transport> transports = java.util.List.of(
        new WebSocketTransport(new StandardWebSocketClient()));

    MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();

    // 역직렬화 트러블 차단
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

    //content1
    Content content1 = new Content(null, null, ContentType.movie, "title1", "description");
    //content2
    Content content2 = new Content(null, null, ContentType.movie, "title2", "description");
    contentRepository.saveAll(List.of(content1, content2));
    content1Id = content1.getId();
    content2Id = content2.getId();

    //user1
    // user 설정
    // user 인증 정보 설정

    user1 = new User("test1@test.com", "test123", "test1", Role.USER);
    //user2
    User user2 = new User("test2@test.com", "test123", "test2", Role.USER);
    //user3
    User user3 = new User("test3@test.com", "test123", "test3", Role.USER);
    userRepository.saveAll(List.of(user1, user2, user3));
    ModuPlyUserDetails moduPlyUserDetails1 = moduPlyUserDetailsService.loadUserByUsername(
        user1.getEmail());
    Authentication authentication1 = new UsernamePasswordAuthenticationToken(moduPlyUserDetails1,
        moduPlyUserDetails1.getAuthorities());
    accessToken1 = jwtTokenProvider.generateAccessToken(authentication1);
    user1Id = user1.getId();

    ModuPlyUserDetails moduPlyUserDetails2 = moduPlyUserDetailsService.loadUserByUsername(
        user2.getEmail());
    Authentication authentication2 = new UsernamePasswordAuthenticationToken(moduPlyUserDetails2,
        moduPlyUserDetails2.getAuthorities());
    accessToken2 = jwtTokenProvider.generateAccessToken(authentication2);
    user2Id = user2.getId();

    ModuPlyUserDetails moduPlyUserDetails3 = moduPlyUserDetailsService.loadUserByUsername(
        user3.getEmail());
    Authentication authentication3 = new UsernamePasswordAuthenticationToken(moduPlyUserDetails3,
        moduPlyUserDetails3.getAuthorities());
    accessToken3 = jwtTokenProvider.generateAccessToken(authentication3);
    user3Id = user3.getId();

    //대화방 구독 user1-content1, user2, user3 -content3
    stompSession1 = connectStompSession(accessToken1);
    String destination1 = "/sub/contents/" + content1Id + "/watch";
    stompSession1.subscribe(destination1, new EmptyFrameHandler());

    stompSession2 = connectStompSession(accessToken2);
    String destination2 = "/sub/contents/" + content2Id + "/watch";
    stompSession2.subscribe(destination2, new EmptyFrameHandler());

    stompSession3 = connectStompSession(accessToken3);
    stompSession3.subscribe(destination2, new EmptyFrameHandler());

    assertThat(awaitSessionPresenceByUserId(user1Id, true)).isNotNull();
    assertThat(awaitSessionPresenceByUserId(user2Id, true)).isNotNull();
    assertThat(awaitSessionPresenceByUserId(user3Id, true)).isNotNull();

  }

  @AfterEach
  void tearDown() throws InterruptedException {
    if (stompSession1 != null) {
      try {
        stompSession1.disconnect();
      } catch (IllegalStateException ignored) {
      }
    }
    if (stompSession2 != null) {
      try {
        stompSession2.disconnect();
      } catch (IllegalStateException ignored) {
      }
    }
    if (stompSession3 != null) {
      try {
        stompSession3.disconnect();
      } catch (IllegalStateException ignored) {
      }
    }

    if (stompClient != null && stompClient.isRunning()) {
      stompClient.stop();
    }
    if (taskScheduler != null) {
      taskScheduler.destroy();
    }
    await().atMost(5, TimeUnit.SECONDS).until(() ->
        (stompSession1 == null || !stompSession1.isConnected())
            && (stompSession2 == null || !stompSession2.isConnected())
            && (stompSession3 == null || !stompSession3.isConnected())
    );
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
    return connectFuture.get(5, TimeUnit.SECONDS);
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

  @Test
  @DisplayName("콘텐츠를 구독한 사용자가 실시간 채팅을 보내고 받는 것에 성공한다.")
  void chat_success_to_content() throws ExecutionException, InterruptedException, TimeoutException {
    //given
    CompletableFuture<ContentChatDto> messageExpectation = new CompletableFuture<>();
    String sd = makeDestination(content1Id, "sub");
    String pd = makeDestination(content1Id, "pub");
    ContentChatSendRequest request = new ContentChatSendRequest("안녕하세요");

    stompSession1.subscribe(sd, new StompFrameHandler() {
      @Override
      public Type getPayloadType(StompHeaders headers) {
        return ContentChatDto.class;
      }

      @Override
      public void handleFrame(StompHeaders headers, Object payload) {
        messageExpectation.complete((ContentChatDto) payload);
      }
    });

    //when
    sendUntilReceived(stompSession1, pd, request, messageExpectation);

    // then
    ContentChatDto receivedMessage = messageExpectation.get(5, TimeUnit.SECONDS);

    assertThat(receivedMessage).isNotNull();
    assertThat(receivedMessage.content()).isEqualTo(request.content());
    assertThat(receivedMessage.sender().userId()).isEqualTo(user1Id);
    assertThat(receivedMessage.sender().name()).isEqualTo("test1");
  }

  @Test
  @DisplayName("콘텐츠를 구독한 사용자가 다른 사용자의 채팅을 받는 것에 성공한다.")
  void receive_chat_success_from_other()
      throws ExecutionException, InterruptedException, TimeoutException {
    //given
    CompletableFuture<ContentChatDto> messageExpectation = new CompletableFuture<>();
    String sd = makeDestination(content2Id, "sub");
    String pd = makeDestination(content2Id, "pub");
    ContentChatSendRequest request = new ContentChatSendRequest("안녕");

    stompSession2.subscribe(sd, new StompFrameHandler() {
      @Override
      public Type getPayloadType(StompHeaders headers) {
        return ContentChatDto.class;
      }

      @Override
      public void handleFrame(StompHeaders headers, Object payload) {
        messageExpectation.complete((ContentChatDto) payload);
      }
    });
    stompSession3.subscribe(sd, new EmptyFrameHandler() {
    });

    //when
    sendUntilReceived(stompSession3, pd, request, messageExpectation);

    // then
    ContentChatDto receivedMessage = messageExpectation.get(5, TimeUnit.SECONDS);

    assertThat(receivedMessage).isNotNull();
    assertThat(receivedMessage.content().equals(request.content())).isTrue();
    assertThat(receivedMessage.sender().userId()).isEqualTo(user3Id);
    assertThat(receivedMessage.sender().name()).isEqualTo("test3");
  }

  @Test
  @DisplayName("구독한 세션과 다른 시청 세션의 대화창을 구독하는 것에 실패한다.")
  void subscribe_chat_fail_with_unsubscribed_content() throws Exception {
    // given
    String incorrectChatDestination = makeDestination(content1Id, "sub");

    // when
    // 잘못된 경로로 구독 요청 (이 패킷이 서버에 도달하면 리스너에서 MessagingException이 터집니다)
    stompSession2.subscribe(incorrectChatDestination, new StompFrameHandler() {
      @Override
      public java.lang.reflect.Type getPayloadType(StompHeaders headers) {
        return Object.class; // 어떤 클래스든 무관
      }

      @Override
      public void handleFrame(StompHeaders headers, Object payload) {
        // 정상 프레임은 수신될 일이 없으므로 비워둡니다.
      }
    });

    // then
    // 서버가 예외를 인지하고 세션을 강제로 drop할 때까지 최대 3초간 대기하며 검증합니다.
    await()
        .atMost(5, TimeUnit.SECONDS)
        .alias("서버가 잘못된 구독 요청을 받았음에도 웹소켓 세션을 끊지 않았습니다.")
        .until(() -> !stompSession2.isConnected());

    // 최종적으로 세션이 완전히 닫혔는지(isConnected == false) 확인합니다.
    assertThat(stompSession2.isConnected()).isFalse();
  }

  @Test
  @DisplayName("구독한 세션이 없을 때 대화창을 구독하는 것에 실패한다.")
  void subscribe_chat_fail_without_session() throws Exception {
    // given
    stompSession1.disconnect();
    assertThat(awaitSessionPresenceByUserId(user1Id, false)).isNull();
    stompSession1 = connectStompSession(accessToken1);//다시 연결

    String incorrectChatDestination = makeDestination(content1Id, "sub");

    // when
    // 잘못된 경로로 구독 요청 (이 패킷이 서버에 도달하면 리스너에서 MessagingException이 터집니다)
    stompSession1.subscribe(incorrectChatDestination, new StompFrameHandler() {
      @Override
      public java.lang.reflect.Type getPayloadType(StompHeaders headers) {
        return Object.class; // 어떤 클래스든 무관
      }

      @Override
      public void handleFrame(StompHeaders headers, Object payload) {
        // 정상 프레임은 수신될 일이 없으므로 비워둡니다.
      }
    });

    // then
    // 서버가 예외를 인지하고 세션을 강제로 drop할 때까지 최대 3초간 대기하며 검증합니다.
    await()
        .atMost(5, TimeUnit.SECONDS)
        .alias("서버가 잘못된 구독 요청을 받았음에도 웹소켓 세션을 끊지 않았습니다.")
        .until(() -> !stompSession1.isConnected());

    // 최종적으로 세션이 완전히 닫혔는지(isConnected == false) 확인합니다.
    assertThat(stompSession1.isConnected()).isFalse();
  }

  @Test
  @DisplayName("콘텐츠를 구독한 사용자의 시청세션이 변경되면 채팅이 전송되지 않는다.")
  void receive_chat_fail_when_change_session()
      throws ExecutionException, InterruptedException, TimeoutException {
    //given
    CompletableFuture<ContentChatDto> messageExpectation = new CompletableFuture<>();
    String sd = makeDestination(content2Id, "sub");
    String pd = makeDestination(content2Id, "pub");
    ContentChatSendRequest request = new ContentChatSendRequest("안녕");

    stompSession2.subscribe(sd, new StompFrameHandler() {
      @Override
      public Type getPayloadType(StompHeaders headers) {
        return ContentChatDto.class;
      }

      @Override
      public void handleFrame(StompHeaders headers, Object payload) {
        messageExpectation.complete((ContentChatDto) payload);
      }
    });
    stompSession3.subscribe(sd, new EmptyFrameHandler() {
    });

    stompSession3.subscribe("/sub/contents/" + content1Id + "/watch", new EmptyFrameHandler() {
    });

    assertThat(awaitSessionContentByUserId(user3Id, content1Id)).isNotNull();

    //when
    stompSession3.send(pd, request);

    // then
    assertThrows(
        TimeoutException.class,
        () -> messageExpectation.get(500, TimeUnit.MILLISECONDS)
    );
  }

  @Test
  @DisplayName("콘텐츠를 구독한 사용자의 시청세션이 소멸(삭제)되면 채팅이 전송되지 않는다.")
  void receive_chat_fail_without_session()
      throws ExecutionException, InterruptedException, TimeoutException {
    //given
    CompletableFuture<ContentChatDto> messageExpectation = new CompletableFuture<>();
    String sd = makeDestination(content2Id, "sub");
    String pd = makeDestination(content2Id, "pub");
    ContentChatSendRequest request = new ContentChatSendRequest("안녕");

    stompSession2.subscribe(sd, new StompFrameHandler() {
      @Override
      public Type getPayloadType(StompHeaders headers) {
        return ContentChatDto.class;
      }

      @Override
      public void handleFrame(StompHeaders headers, Object payload) {
        messageExpectation.complete((ContentChatDto) payload);
      }
    });
    stompSession3.subscribe(sd, new EmptyFrameHandler() {
    });

    assertThat(awaitSessionPresenceByUserId(user3Id, true)).isNotNull();

    // 💡 해결책 2: 혹시 모를 타이밍을 위해 orElseThrow() 대신, 세션을 안전하게 조회하도록 보장
    WatchingSession session = watchingSessionRepository.findByUserId(user3Id)
        .orElseThrow(
            () -> new AssertionError("비동기 세션 저장이 대기 시간 내에 완료되지 않았습니다. 현재 DB에 유저의 세션이 없습니다."));

    //기존 세션 삭제
    watchingSessionRepository.deleteBySessionId(session.getSessionId());

    assertThat(awaitSessionPresenceByUserId(user3Id, false)).isNull();

    //when
    stompSession3.send(pd, request);

    // then
    assertThrows(
        TimeoutException.class,
        () -> messageExpectation.get(500, TimeUnit.MILLISECONDS)
    );
  }

  @Test
  @DisplayName("빈 채팅 메세지의 경우 전송되지 않는다.")
  void receive_chat_fail_with_blank_message()
      throws ExecutionException, InterruptedException, TimeoutException {
    //given
    CompletableFuture<ContentChatDto> messageExpectation = new CompletableFuture<>();
    String sd = makeDestination(content2Id, "sub");
    String pd = makeDestination(content2Id, "pub");
    ContentChatSendRequest request = new ContentChatSendRequest("");

    stompSession2.subscribe(sd, new StompFrameHandler() {
      @Override
      public Type getPayloadType(StompHeaders headers) {
        return ContentChatDto.class;
      }

      @Override
      public void handleFrame(StompHeaders headers, Object payload) {
        messageExpectation.complete((ContentChatDto) payload);
      }
    });
    stompSession3.subscribe(sd, new EmptyFrameHandler() {
    });

    assertThat(awaitSessionPresenceByUserId(user3Id, true)).isNotNull();

    //when
    stompSession3.send(pd, request);

    // then
    assertThrows(
        TimeoutException.class,
        () -> messageExpectation.get(500, TimeUnit.MILLISECONDS)
    );
  }


  private String makeDestination(UUID contentId, String header) {
    return "/" + header + "/contents/" + contentId + "/chat";
  }

  // 저장소에서 사용자 아이디로 조회
  private WatchingSession awaitSessionPresenceByUserId(UUID userId, boolean expectVisible)
      throws InterruptedException {
    long deadline = System.currentTimeMillis() + 5000;
    while (System.currentTimeMillis() < deadline) {
      var opt = watchingSessionRepository.findByUserId(userId);
      if (opt.isPresent() == expectVisible) {
        return opt.orElse(null);
      }
      Thread.sleep(100);
    }
    return watchingSessionRepository.findByUserId(userId).orElse(null);
  }

  private static final Duration SUBSCRIBE_SYNC_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration SUBSCRIBE_SYNC_POLL_INTERVAL = Duration.ofMillis(100);

  private void sendUntilReceived(StompSession session, String destination,
      ContentChatSendRequest request, CompletableFuture<?> receivedSignal)
      throws InterruptedException, ExecutionException, TimeoutException {
    await().atMost(SUBSCRIBE_SYNC_TIMEOUT)
        .pollInterval(SUBSCRIBE_SYNC_POLL_INTERVAL)
        .until(() -> {
          if (!receivedSignal.isDone()) {
            session.send(destination, request);
          }
          return receivedSignal.isDone();
        });
  }

  private WatchingSession awaitSessionContentByUserId(UUID userId, UUID contentId)
      throws InterruptedException {
    long deadline = System.currentTimeMillis() + 5000;
    while (System.currentTimeMillis() < deadline) {
      var opt = watchingSessionRepository.findByUserId(userId);
      if (opt.isPresent() && contentId.equals(opt.get().getContentId())) {
        return opt.get();
      }
      Thread.sleep(100);
    }
    return null;
  }
}
