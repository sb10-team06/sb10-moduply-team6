package com.team6.moduply.watching.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team6.moduply.config.support.IntegrationTestSupport;
import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.enums.ContentType;
import com.team6.moduply.content.repository.ContentRepository;
import com.team6.moduply.user.dto.UserSummary;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.enums.Role;
import com.team6.moduply.user.repository.UserRepository;
import com.team6.moduply.watching.model.WatchingSession;
import com.team6.moduply.watching.repository.WatchingSessionRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class WatchingSessionApiIntegrationTest extends IntegrationTestSupport {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private WatchingSessionRepository watchingSessionRepository;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private ContentRepository contentRepository;

  private UUID user1Id;
  private UUID user2Id;
  private UUID user3Id;
  private User user1;
  private UUID content1Id;
  private UUID content2Id;
  private String sessionId1;
  private UUID watchingSessionId1;
  private UUID watchingSessionId2;
  private UUID watchingSessionId3;
  // TODO: [김민형] 목록 조회 테스트 추가 예정으로, 아직 사용되지 않은 변수가 있을 수 있습니다.

  @BeforeEach
  void setUp() {
    contentRepository.deleteAll();
    userRepository.deleteAll();

    //content1
    Content content1 = new Content(null, null, ContentType.movie, "title1", "description");
    //content2
    Content content2 = new Content(null, null, ContentType.movie, "title2", "description");
    contentRepository.saveAll(List.of(content1, content2));
    content1Id = content1.getId();
    content2Id = content2.getId();

    //user1
    user1 = new User("test1@test.com", "test123", "test1", Role.USER);
    //user2
    User user2 = new User("test2@test.com", "test123", "test2", Role.USER);
    //user3
    User user3 = new User("test3@test.com", "test123", "test3", Role.USER);
    userRepository.saveAll(List.of(user1, user2, user3));
    user1Id = user1.getId();
    user2Id = user2.getId();
    user3Id = user3.getId();

    //watching session
    sessionId1 = UUID.randomUUID().toString();
    String sessionId2 = UUID.randomUUID().toString();
    String sessionId3 = UUID.randomUUID().toString();

    //user1-content1, user2,user3-content2
    UserSummary watcher1 = new UserSummary(user1.getId(), user1.getName(), null);
    UserSummary watcher2 = new UserSummary(user2.getId(), user2.getName(), null);
    UserSummary watcher3 = new UserSummary(user3.getId(), user3.getName(), null);
    WatchingSession watchingSession1 = WatchingSession.create(sessionId1, watcher1, content1Id);
    watchingSessionRepository.save(watchingSession1);
    watchingSessionId1 = watchingSession1.getId();
    WatchingSession watchingSession2 = WatchingSession.create(sessionId2, watcher2, content2Id);
    watchingSessionRepository.save(watchingSession2);
    watchingSessionId2 = watchingSession2.getId();
    WatchingSession watchingSession3 = WatchingSession.create(sessionId3, watcher3, content2Id);
    watchingSessionRepository.save(watchingSession3);
    watchingSessionId3 = watchingSession3.getId();
  }

  @Test
  @WithUserDetails(value = "test2@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
  @DisplayName("사용자 아이디로 해당 사용자의 현재 시청세션을 조회하는데 성공합니다.")
  void find_watching_session_success_by_watcher_id() throws Exception {
    //when & then
    mockMvc.perform(get("/api/users/{watcherId}/watching-sessions", user1Id)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(watchingSessionId1.toString()))
        .andExpect(jsonPath("$.watcher.userId").value(user1Id.toString()))
        .andExpect(jsonPath("$.content.id").value(content1Id.toString()));
  }

  @Test
  @WithUserDetails(value = "test1@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
  @DisplayName("사용자 아이디로 아무것도 보고 있지 않은 사용자의 시청 세션을 조회하는데 성공합니다.(null)")
  void find_null_success_by_watcher_id() throws Exception {
    watchingSessionRepository.deleteBySessionId(sessionId1);
    //when & then
    mockMvc.perform(get("/api/users/{watcherId}/watching-sessions", user1Id)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());
  }

  @Test
  @WithUserDetails(value = "test1@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
  @DisplayName("존재하지 않는 콘텐츠의 시청 세션 조회에 실패합니다.")
  void find_fail_when_content_removed() throws Exception {
    contentRepository.deleteById(content1Id);
    //when & then
    mockMvc.perform(get("/api/users/{watcherId}/watching-sessions", user1Id)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.exceptionType").value("ContentException"));
  }

  @Test
  @DisplayName("인증되지 않은 요청은 401을 반환합니다.")
  void find_fail_by_unauthenticated_request() throws Exception {
    mockMvc.perform(get("/api/users/{watcherId}/watching-sessions", user1Id)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

}
