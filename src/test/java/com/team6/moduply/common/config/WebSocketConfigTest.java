package com.team6.moduply.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.team6.moduply.auth.JwtTokenProvider;
import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.auth.userdetails.ModuPlyUserDetailsService;
import com.team6.moduply.user.dto.UserCreateRequest;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.enums.Role;
import com.team6.moduply.user.repository.UserRepository;
import com.team6.moduply.user.service.UserService;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Tag("integration")
@Slf4j
class WebSocketConfigTest {

  // 톰캣 서버 포트 번호
  @LocalServerPort
  private int port;

  private WebSocketStompClient stompClient;
  private String webSocketUrl;
  private String userAccessToken;
  private String userRefreshToken;
  private String adminAccessToken;

  @Autowired
  private UserService userService;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private JwtTokenProvider jwtTokenProvider;
  @Autowired
  private ModuPlyUserDetailsService moduPlyUserDetailsService;

  // 가상 브라우저 설정
  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
    // URL 생성
    this.webSocketUrl = "http://localhost:" + port + "/ws";
    // 웹소켓 통신 장치
    List<Transport> transports = List.of(new WebSocketTransport(new StandardWebSocketClient()));
    // SocketJS 지원 브라우저로 설정
    SockJsClient sockJsClient = new SockJsClient(transports);

    // STOMP 클라이언트 초기화(채팅)
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
    userAccessToken = jwtTokenProvider.generateAccessToken(authentication);
    userRefreshToken = jwtTokenProvider.generateRefreshToken(authentication);

    //admin
    userService.createUser(
        new UserCreateRequest("admin", "admin@test.com", "test1234!"));
    User adminUser = userRepository.findByEmail("admin@test.com").orElse(null);
    assertThat(adminUser).isNotNull();
    adminUser.updateRole(Role.ADMIN);
    ModuPlyUserDetails adminDetails = moduPlyUserDetailsService.loadUserByUsername(
        adminUser.getEmail());
    assertThat(adminDetails).isNotNull();
    Authentication adminAuth = new UsernamePasswordAuthenticationToken(adminDetails,
        adminDetails.getAuthorities());
    adminAccessToken = jwtTokenProvider.generateAccessToken(adminAuth);
  }

  @Test
  @DisplayName("유효한 토큰으로 웹소켓 핸드쉐이크 및 STOMP 연결을 성공한다.(User 권한)")
  void connect_success_websocket_with_access_token_user() throws Exception {
    // given
    // 비동기 작업의 결과를 완료 처리하기 위한 객체 (테스트 쓰레드 대기용)
    CompletableFuture<StompSession> completableFuture = new CompletableFuture<>();

    // 허용한 오리진(STOMP 혜더)
    StompHeaders stompHeaders = new StompHeaders();
    stompHeaders.add("Authorization", "Bearer " + userAccessToken);
    stompHeaders.add("Origin", "http://localhost");
    // HTTP 헤더
    WebSocketHttpHeaders webSocketHttpHeaders = new WebSocketHttpHeaders();
    webSocketHttpHeaders.add("Origin", "http://localhost");

    // when
    // 엔드포인트 연결 요청
    stompClient.connectAsync(
        webSocketUrl,
        webSocketHttpHeaders,
        stompHeaders,
        new StompSessionHandlerAdapter() {
          @Override
          public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            // 연결 성공 시 세션 저장 후 완료
            completableFuture.complete(session);
          }
        }
    );

    // then
    // 3초 대기
    StompSession stompSession = completableFuture.get(3, TimeUnit.SECONDS);

    assertThat(stompSession).isNotNull();
    assertThat(stompSession.isConnected()).isTrue();

    // 커넥션 세션 종료
    stompSession.disconnect();
  }

  @Test
  @DisplayName("유효한 토큰으로 웹소켓 핸드쉐이크 및 STOMP 연결을 성공한다.(Admin 권한)")
  void connect_success_websocket_with_access_token_admin() throws Exception {
    // given
    // 비동기 작업의 결과를 완료 처리하기 위한 객체 (테스트 쓰레드 대기용)
    CompletableFuture<StompSession> completableFuture = new CompletableFuture<>();

    // 허용한 오리진(STOMP 혜더)
    StompHeaders stompHeaders = new StompHeaders();
    stompHeaders.add("Authorization", "Bearer " + adminAccessToken);
    stompHeaders.add("Origin", "http://localhost");
    // HTTP 헤더
    WebSocketHttpHeaders webSocketHttpHeaders = new WebSocketHttpHeaders();
    webSocketHttpHeaders.add("Origin", "http://localhost");

    // when
    // 엔드포인트 연결 요청
    stompClient.connectAsync(
        webSocketUrl,
        webSocketHttpHeaders,
        stompHeaders,
        new StompSessionHandlerAdapter() {
          @Override
          public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            // 연결 성공 시 세션 저장 후 완료
            completableFuture.complete(session);
          }
        }
    );

    // then
    // 3초 대기
    StompSession stompSession = completableFuture.get(3, TimeUnit.SECONDS);

    assertThat(stompSession).isNotNull();
    assertThat(stompSession.isConnected()).isTrue();

    // 커넥션 세션 종료
    stompSession.disconnect();
  }

  @Test
  @DisplayName("유효하지 않은 토큰으로 웹소켓 핸드쉐이크 및 STOMP 연결을 실패한다.")
  void connect_fail_websocket_with_invalid_access_token() throws Exception {
    // given

    // 허용한 오리진(STOMP 혜더)
    StompHeaders stompHeaders = new StompHeaders();
    // 잘못된 토큰(리프레시토큰으로 접근)
    stompHeaders.add("Authorization", "Bearer " + userRefreshToken);
    stompHeaders.add("Origin", "http://localhost");
    // HTTP 헤더
    WebSocketHttpHeaders webSocketHttpHeaders = new WebSocketHttpHeaders();
    webSocketHttpHeaders.add("Origin", "http://localhost");

    // when
    // 엔드포인트 연결 요청
    Future<StompSession> connectionFuture = stompClient.connectAsync(
        webSocketUrl,
        webSocketHttpHeaders,
        stompHeaders,
        new StompSessionHandlerAdapter() {
        }
    );

    // then
    // 3초 대기
    Throwable throwable = assertThrows(
        ExecutionException.class,
        () -> connectionFuture.get(3, TimeUnit.SECONDS));
    assertThat(throwable.getCause().getMessage()).contains("Connection closed");
  }

  @Test
  @DisplayName("토큰없이 웹소켓 핸드쉐이크 및 STOMP 연결을 실패한다.")
  void connect_fail_websocket_without_access_token() throws Exception {
    // given

    // 허용한 오리진(STOMP 혜더)
    StompHeaders stompHeaders = new StompHeaders();
    stompHeaders.add("Origin", "http://localhost");
    // HTTP 헤더
    WebSocketHttpHeaders webSocketHttpHeaders = new WebSocketHttpHeaders();
    webSocketHttpHeaders.add("Origin", "http://localhost");

    // when
    // 엔드포인트 연결 요청
    Future<StompSession> connectionFuture = stompClient.connectAsync(
        webSocketUrl,
        webSocketHttpHeaders,
        stompHeaders,
        new StompSessionHandlerAdapter() {
        }
    );

    // then
    // 3초 대기
    Throwable throwable = assertThrows(
        ExecutionException.class,
        () -> connectionFuture.get(3, TimeUnit.SECONDS));
    assertThat(throwable.getCause().getMessage()).contains("Connection closed");
  }


  @Test
  @DisplayName("허용하지 않은 오리진으로 웹소켓 연결 요청이 금지된다.")
  void connect_fail_websocket_with_forbiddenOrigin() throws Exception {
    // given

    // 허용하지 않은 오리진(STOMP 혜더)
    StompHeaders stompHeaders = new StompHeaders();
    stompHeaders.add("Authorization", "Bearer " + userAccessToken);
    stompHeaders.add("Origin", "http://wronghost");
    // HTTP 헤더
    WebSocketHttpHeaders webSocketHttpHeaders = new WebSocketHttpHeaders();
    webSocketHttpHeaders.add("Origin", "http://wronghost");

    // when
    // 엔드포인트 연결 요청
    Future<StompSession> connectionFuture = stompClient.connectAsync(
        webSocketUrl,
        webSocketHttpHeaders,
        stompHeaders,
        new StompSessionHandlerAdapter() {
        }
    );

    // then
    // 3초 대기
    Throwable throwable = assertThrows(
        ExecutionException.class,
        () -> connectionFuture.get(3, TimeUnit.SECONDS));
    assertThat(throwable.getCause().getMessage()).contains("403");
  }
}