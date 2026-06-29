package com.team6.moduply.follow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team6.moduply.auth.filter.JwtAuthenticationFilter;
import com.team6.moduply.auth.userdetails.ModuPlyUserDetails;
import com.team6.moduply.follow.dto.FollowDto;
import com.team6.moduply.follow.dto.FollowRequest;
import com.team6.moduply.follow.exception.FollowErrorCode;
import com.team6.moduply.follow.exception.FollowException;
import com.team6.moduply.follow.service.FollowService;
import com.team6.moduply.user.dto.UserDto;
import com.team6.moduply.user.enums.Role;
import com.team6.moduply.user.exception.UserErrorCode;
import com.team6.moduply.user.exception.UserException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    /// FollowController만 테스트
    /// Service, Repository, DB 로딩x
    controllers = FollowController.class,
    /// JwtAuthenticationFilter 제외: JWT토큰 검증이 아니라 가짜 인증 사용자를 넣어서 테스트.
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = JwtAuthenticationFilter.class
    )
)
class FollowControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private FollowService followService;

  @Test
  @DisplayName("팔로우 생성 요청 시 인증 사용자 ID를 서비스에 전달하고 201을 반환한다.")
  void createFollow_success_with_authenticated_principal() throws Exception {
    // given
    // 팔로우를 걸 사람: ex) 민수
    UUID followerId = UUID.randomUUID();
    // 팔로우를 당할사람: ex) 영희
    UUID followeeId = UUID.randomUUID();
    // (민수, 영희) 팔로우 관계 id: ex) aaaa-aaaa-aaaa-aaaa
    UUID followId = UUID.randomUUID();
    FollowRequest request = new FollowRequest(followeeId);
    FollowDto response = new FollowDto(followId, followerId, followeeId);

    // followService.createFollow()의 응답이 response가 될것이다.
    given(followService.createFollow(eq(request), eq(followerId))).willReturn(response);

    // when & then
    // post /api/follows 요청
    mockMvc.perform(post("/api/follows")
            // 인증 사용자로  follwerId를 가진 ModuPlyUserDetails 주입
            .with(user(userDetails(followerId)))
            // csrf토큰 추가
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            // 요청 바디로 FollowRequest 전달
            .content(objectMapper.writeValueAsString(request)))
            // 결과: 201 Created 나와야한다.
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(followId.toString()))
            // followerId도 인증 사용자 ID와 같아야한다.
        .andExpect(jsonPath("$.followerId").value(followerId.toString()))
        .andExpect(jsonPath("$.followeeId").value(followeeId.toString()));

    // followService.createFollow()가 실행되었는지?
    verify(followService).createFollow(request, followerId);
  }

  @Test
  @DisplayName("팔로우 취소 요청 시 인증 사용자 ID를 서비스에 전달하고 204를 반환한다.")
  void cancelFollow_success_with_authenticated_principal() throws Exception {
    // given
    UUID followId = UUID.randomUUID();
    UUID followerId = UUID.randomUUID();

    // when & then
    mockMvc.perform(delete("/api/follows/{followId}", followId)
            .with(user(userDetails(followerId)))
            .with(csrf()))
        .andExpect(status().isNoContent());

    verify(followService).cancelFollow(followId, followerId);
  }

  @Test
  @DisplayName("특정 유저를 내가 팔로우하는지 조회하면 인증 사용자 ID를 서비스에 전달하고 200을 반환한다.")
  void isFollowedByMe_success_with_authenticated_principal() throws Exception {
    // given
    UUID followId = UUID.randomUUID();
    UUID followerId = UUID.randomUUID();
    UUID followeeId = UUID.randomUUID();
    FollowDto response = new FollowDto(followId, followerId, followeeId);

    given(followService.isFollowedByMe(eq(followeeId), eq(followerId))).willReturn(response);

    // when & then
    mockMvc.perform(get("/api/follows/followed-by-me")
            .with(user(userDetails(followerId)))
            .param("followeeId", followeeId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(followId.toString()))
        .andExpect(jsonPath("$.followerId").value(followerId.toString()))
        .andExpect(jsonPath("$.followeeId").value(followeeId.toString()));

    verify(followService).isFollowedByMe(followeeId, followerId);
  }

  @Test
  @DisplayName("특정 유저를 내가 팔로우하지 않으면 404를 반환한다.")
  void isFollowedByMe_fail_when_follow_not_found() throws Exception {
    // given
    // 내 ID
    UUID followerId = UUID.randomUUID();
    // 조회할 사용자 ID
    UUID followeeId = UUID.randomUUID();
    // followService.isFollowedByMe를 예외발생하도록 준비
    given(followService.isFollowedByMe(eq(followeeId), eq(followerId)))
        .willThrow(new FollowException(
            FollowErrorCode.FOLLOW_NOT_FOUND,
            Map.of("followerId", followerId, "followeeId", followeeId)
        ));

    // when & then
    mockMvc.perform(get("/api/follows/followed-by-me")
            .with(user(userDetails(followerId)))
            .param("followeeId", followeeId.toString()))
            // 기대: 404
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(FollowErrorCode.FOLLOW_NOT_FOUND.getCode()))
        .andExpect(jsonPath("$.exceptionType").value("FollowException"));

    verify(followService).isFollowedByMe(followeeId, followerId);
  }

  @Test
  @DisplayName("자기 자신을 팔로우 여부 조회하면 404를 반환한다.")
  void isFollowedByMe_fail_when_followee_is_self() throws Exception {
    // given
    UUID userId = UUID.randomUUID();

    given(followService.isFollowedByMe(eq(userId), eq(userId)))
        .willThrow(new FollowException(
            FollowErrorCode.FOLLOW_NOT_FOUND,
            Map.of("followerId", userId, "followeeId", userId)
        ));

    // when & then
    mockMvc.perform(get("/api/follows/followed-by-me")
            .with(user(userDetails(userId)))
            .param("followeeId", userId.toString()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(FollowErrorCode.FOLLOW_NOT_FOUND.getCode()))
        .andExpect(jsonPath("$.exceptionType").value("FollowException"));

    verify(followService).isFollowedByMe(userId, userId);
  }

  @Test
  @DisplayName("특정 유저의 팔로워 수를 조회하면 200과 팔로워 수를 반환한다.")
  void getFollowerCount_success() throws Exception {
    // given
    UUID followeeId = UUID.randomUUID();
    given(followService.getFollowerCount(followeeId)).willReturn(3L);

    // when & then
    mockMvc.perform(get("/api/follows/count")
            .with(user(userDetails(UUID.randomUUID())))
            .param("followeeId", followeeId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(3L));

    verify(followService).getFollowerCount(followeeId);
  }

  @Test
  @DisplayName("존재하지 않는 유저의 팔로워 수를 조회하면 404를 반환한다.")
  void getFollowerCount_fail_when_followee_not_found() throws Exception {
    // given
    UUID followeeId = UUID.randomUUID();
    // 존재하지 않은 유저 팔로워 수 조회 USER_NOT_FOUND 셋팅
    given(followService.getFollowerCount(followeeId))
        .willThrow(new UserException(
            UserErrorCode.USER_NOT_FOUND_EXCEPTION,
            Map.of("userId", followeeId)
        ));

    // when & then
    mockMvc.perform(get("/api/follows/count")
            .with(user(userDetails(UUID.randomUUID())))
            .param("followeeId", followeeId.toString()))
            // 기대: 404
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(UserErrorCode.USER_NOT_FOUND_EXCEPTION.getCode()))
        .andExpect(jsonPath("$.exceptionType").value("UserException"));

    verify(followService).getFollowerCount(followeeId);
  }

  @Test
  @DisplayName("잘못된 UUID 형식으로 팔로워 수를 조회하면 400을 반환한다.")
  void getFollowerCount_fail_with_invalid_followee_id_format() throws Exception {
    // when & then
    mockMvc.perform(get("/api/follows/count")
            .with(user(userDetails(UUID.randomUUID())))
            .param("followeeId", "invalid-uuid"))
        .andExpect(status().isBadRequest());

    verify(followService, never()).getFollowerCount(any(UUID.class));
  }

  private ModuPlyUserDetails userDetails(UUID userId) {
    UserDto userDto = new UserDto(
        userId,
        null,
        "user@example.com",
        "user",
        null,
        Role.USER,
        false
    );
    return new ModuPlyUserDetails(userDto, "encoded-password");
  }
}
