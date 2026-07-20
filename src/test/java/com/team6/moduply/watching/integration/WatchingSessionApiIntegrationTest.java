package com.team6.moduply.watching.integration;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.config.support.IntegrationTestSupport;
import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.enums.ContentType;
import com.team6.moduply.content.repository.ContentRepository;
import com.team6.moduply.user.dto.UserSummary;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.enums.Role;
import com.team6.moduply.user.repository.UserRepository;
import com.team6.moduply.watching.dto.WatchingSessionQueryCondition;
import com.team6.moduply.watching.enums.WatchingSessionSortBy;
import com.team6.moduply.watching.model.WatchingSession;
import com.team6.moduply.watching.repository.WatchingSessionRepository;
import java.util.List;
import java.util.Map;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Transactional
public abstract class WatchingSessionApiIntegrationTest extends IntegrationTestSupport {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private WatchingSessionRepository watchingSessionRepository;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private ContentRepository contentRepository;
  @Autowired
  private ObjectMapper objectMapper;

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
  private WatchingSession watchingSession2;
  private WatchingSession watchingSession3;

  @BeforeEach
  void setUp() {
    /*
     * 이 테스트는 고정 email(test1@test.com 등)을 매번 재사용한다.
     * deleteAll()만 호출하면 같은 트랜잭션 안에서 Hibernate가 insert를 delete보다 먼저 flush할 수 있어
     * users_email_key unique 제약 조건에 걸린다. 배치 삭제를 즉시 실행해 테스트 간 데이터를 격리한다.
     */
    contentRepository.deleteAllInBatch();
    userRepository.deleteAllInBatch();

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
    watchingSession2 = WatchingSession.create(sessionId2, watcher2, content2Id);
    watchingSessionRepository.save(watchingSession2);
    watchingSessionId2 = watchingSession2.getId();
    try {
      Thread.sleep(1);//시간차
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    watchingSession3 = WatchingSession.create(sessionId3, watcher3, content2Id);
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
  @DisplayName("시청세션 단건 조회에서 인증되지 않은 요청은 401을 반환합니다.")
  void find_fail_by_unauthenticated_request() throws Exception {
    mockMvc.perform(get("/api/users/{watcherId}/watching-sessions", user1Id)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("시청세션 목록 조회에서 인증되지 않은 요청은 401을 반환합니다.")
  void find_all_fail_by_unauthenticated_request() throws Exception {
    mockMvc.perform(get("/api/contents/{contentId}/watching-sessions", content1Id)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }


  @Test
  @WithUserDetails(value = "test1@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
  @DisplayName("존재하지 않는 콘텐츠의 시청 세션 목록 조회에 실패합니다.")
  void find_all_fail_when_content_removed() throws Exception {
    contentRepository.deleteById(content1Id);

    WatchingSessionQueryCondition condition = new WatchingSessionQueryCondition(
        null,
        null,
        null,
        1,
        SortDirection.ASCENDING,
        WatchingSessionSortBy.createdAt
    );

    //when & then
    mockMvc.perform(get("/api/contents/{contentId}/watching-sessions", content1Id)
            .params(convertToParams(condition))
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.exceptionType").value("ContentException"));
  }

  @Test
  @WithUserDetails(value = "test1@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
  @DisplayName("필수 쿼리 조건으로 시청 세션 목록 조회에 성공합니다.(다음 목록이 없는 경우)")
  void find_all_success_with_required_condition() throws Exception {

    WatchingSessionQueryCondition condition = new WatchingSessionQueryCondition(
        null,
        null,
        null,
        2,
        SortDirection.ASCENDING,
        WatchingSessionSortBy.createdAt
    );

    //when & then
    mockMvc.perform(get("/api/contents/{contentId}/watching-sessions", content2Id)
            .params(convertToParams(condition))
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.size()").value(2))
        .andExpect(jsonPath("$.data[0].id").value(watchingSessionId2.toString()))
        .andExpect(jsonPath("$.data[1].id").value(watchingSessionId3.toString()))
        .andExpect(jsonPath("$.data[0].content.id").value(content2Id.toString()))
        .andExpect(jsonPath("$.data[0].watcher.userId").value(user2Id.toString()))
        .andExpect(jsonPath("$.data[1].watcher.userId").value(user3Id.toString()))
        .andExpect(jsonPath("$.nextCursor").value(nullValue()))
        .andExpect(jsonPath("$.nextIdAfter").value(nullValue()))
        .andExpect(jsonPath("$.hasNext").value(false))
        .andExpect(jsonPath("$.totalCount").value(2))
        .andExpect(jsonPath("$.sortBy").value(condition.sortBy().toString()))
        .andExpect(jsonPath("$.sortDirection").value(condition.sortDirection().toString()));
  }

  @Test
  @WithUserDetails(value = "test1@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
  @DisplayName("필수 쿼리 조건으로 시청 세션 목록 조회에 성공합니다.(다음 목록이 있는 경우)")
  void find_all_success_with_required_condition_and_return_next_cursor() throws Exception {

    WatchingSessionQueryCondition condition = new WatchingSessionQueryCondition(
        null,
        null,
        null,
        1,
        SortDirection.DESCENDING,
        WatchingSessionSortBy.createdAt
    );

    //when & then
    mockMvc.perform(get("/api/contents/{contentId}/watching-sessions", content2Id)
            .params(convertToParams(condition))
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.size()").value(1))
        .andExpect(jsonPath("$.data[0].id").value(watchingSessionId3.toString()))
        .andExpect(jsonPath("$.data[0].content.id").value(content2Id.toString()))
        .andExpect(jsonPath("$.data[0].watcher.userId").value(user3Id.toString()))
        .andExpect(jsonPath("$.nextCursor").value(watchingSession3.getCreatedAt().toString()))
        .andExpect(jsonPath("$.nextIdAfter").value(watchingSessionId3.toString()))
        .andExpect(jsonPath("$.hasNext").value(true))
        .andExpect(jsonPath("$.totalCount").value(2))
        .andExpect(jsonPath("$.sortBy").value(condition.sortBy().toString()))
        .andExpect(jsonPath("$.sortDirection").value(condition.sortDirection().toString()));
  }

  @Test
  @WithUserDetails(value = "test1@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
  @DisplayName("커서 조건으로 시청 세션 목록 조회에 성공합니다")
  void find_all_success_with_cursor() throws Exception {

    WatchingSessionQueryCondition condition = new WatchingSessionQueryCondition(
        null,
        watchingSession3.getCreatedAt().toString(),
        watchingSessionId3,
        1,
        SortDirection.DESCENDING,
        WatchingSessionSortBy.createdAt
    );

    //when & then
    mockMvc.perform(get("/api/contents/{contentId}/watching-sessions", content2Id)
            .params(convertToParams(condition))
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.size()").value(1))
        .andExpect(jsonPath("$.data[0].id").value(watchingSessionId2.toString()))
        .andExpect(jsonPath("$.data[0].content.id").value(content2Id.toString()))
        .andExpect(jsonPath("$.data[0].watcher.userId").value(user2Id.toString()))
        .andExpect(jsonPath("$.nextCursor").value(nullValue()))
        .andExpect(jsonPath("$.nextIdAfter").value(nullValue()))
        .andExpect(jsonPath("$.hasNext").value(false))
        .andExpect(jsonPath("$.totalCount").value(2))
        .andExpect(jsonPath("$.sortBy").value(condition.sortBy().toString()))
        .andExpect(jsonPath("$.sortDirection").value(condition.sortDirection().toString()));
  }

  @Test
  @WithUserDetails(value = "test1@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
  @DisplayName("이름 조건으로 시청 세션 목록 조회에 성공합니다")
  void find_all_success_with_watcher_name_like() throws Exception {

    WatchingSessionQueryCondition condition = new WatchingSessionQueryCondition(
        "t2",
        null,
        null,
        10,
        SortDirection.ASCENDING,
        WatchingSessionSortBy.createdAt
    );

    //when & then
    mockMvc.perform(get("/api/contents/{contentId}/watching-sessions", content2Id)
            .params(convertToParams(condition))
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.size()").value(1))
        .andExpect(jsonPath("$.data[0].id").value(watchingSessionId2.toString()))
        .andExpect(jsonPath("$.data[0].content.id").value(content2Id.toString()))
        .andExpect(jsonPath("$.data[0].watcher.userId").value(user2Id.toString()))
        .andExpect(jsonPath("$.nextCursor").value(nullValue()))
        .andExpect(jsonPath("$.nextIdAfter").value(nullValue()))
        .andExpect(jsonPath("$.hasNext").value(false))
        .andExpect(jsonPath("$.totalCount").value(1))
        .andExpect(jsonPath("$.sortBy").value(condition.sortBy().toString()))
        .andExpect(jsonPath("$.sortDirection").value(condition.sortDirection().toString()));
  }

  @Test
  @WithUserDetails(value = "test1@test.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
  @DisplayName("이름 조건과 커서조건으로 시청 세션 목록 조회에 성공합니다")
  void find_all_success_with_watcher_name_like_and_cursor() throws Exception {

    WatchingSessionQueryCondition condition = new WatchingSessionQueryCondition(
        "test",
        watchingSession2.getCreatedAt().toString(),
        watchingSessionId2,
        10,
        SortDirection.ASCENDING,
        WatchingSessionSortBy.createdAt
    );

    //when & then
    mockMvc.perform(get("/api/contents/{contentId}/watching-sessions", content2Id)
            .params(convertToParams(condition))
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.size()").value(1))
        .andExpect(jsonPath("$.data[0].id").value(watchingSessionId3.toString()))
        .andExpect(jsonPath("$.data[0].content.id").value(content2Id.toString()))
        .andExpect(jsonPath("$.data[0].watcher.userId").value(user3Id.toString()))
        .andExpect(jsonPath("$.nextCursor").value(nullValue()))
        .andExpect(jsonPath("$.nextIdAfter").value(nullValue()))
        .andExpect(jsonPath("$.hasNext").value(false))
        .andExpect(jsonPath("$.totalCount").value(2))
        .andExpect(jsonPath("$.sortBy").value(condition.sortBy().toString()))
        .andExpect(jsonPath("$.sortDirection").value(condition.sortDirection().toString()));
  }

  private MultiValueMap<String, String> convertToParams(Object dto) {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    Map<String, String> map = objectMapper.convertValue(dto,
        new TypeReference<>() {
        });
    //null 제외
    map.entrySet().stream()
        .filter(entry -> entry.getValue() != null)
        .forEach(entry -> params.add(entry.getKey(), entry.getValue()));

    return params;
  }
}
