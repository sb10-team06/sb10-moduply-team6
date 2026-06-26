package com.team6.moduply.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, //실제 톰갯 서버 구동
    properties = { // 시큐리티 자동 설정 제외
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration"
    })
@ActiveProfiles("test")
@Tag("integration")
@Slf4j
class WebSocketConfigTest {

  // 콤캣 서버 포트 번호
  @LocalServerPort
  private int port;

  private WebSocketStompClient stompClient;
  private String webSocketUrl;

  // 가상 브라우저 설정
  @BeforeEach
  void setUp() {
    // URL 생성
    this.webSocketUrl = "http://localhost:" + port + "/ws";
    // 웹소켓 통신 장치
    List<Transport> transports = List.of(new WebSocketTransport(new StandardWebSocketClient()));
    // SocketJS 지원 브라우저로 설정
    SockJsClient sockJsClient = new SockJsClient(transports);

    // STOMP 클라이언트 초기화(채팅)
    this.stompClient = new WebSocketStompClient(sockJsClient);
  }

  @Test
  @DisplayName("웹소켓 핸드쉐이크 및 STOMP 연결을 성공한다.")
  void connectWebsocket() throws Exception {
    // given
    // 비동기 작업의 결과를 완료 처리하기 위한 객체 (테스트 쓰레드 대기용)
    CompletableFuture<StompSession> completableFuture = new CompletableFuture<>();

    // 허용한 오리진(STOMP 혜더)
    StompHeaders stompHeaders = new StompHeaders();
    String dummyUserId = "00000000-0000-0000-0000-000000000001";
    stompHeaders.add("Authorization", "Bearer " + dummyUserId);
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
  @DisplayName("허용하지 않은 오리진으로 웹소켓 연결 요청이 금지된다.")
  void getErrorToConnectWebsocketWithForbiddenOrigin() throws Exception {
    // given

    // 허용하지 않은 오리진(STOMP 혜더)
    StompHeaders stompHeaders = new StompHeaders();
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