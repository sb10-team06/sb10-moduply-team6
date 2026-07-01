package com.team6.moduply.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.team6.moduply.common.config.JpaAuditingConfig;
import com.team6.moduply.common.config.QueryDslConfig;
import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.config.support.RepositoryTestSupport;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.enums.Role;
import com.team6.moduply.user.enums.UserSortBy;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({JpaAuditingConfig.class, QueryDslConfig.class})
class UserRepositoryTest extends RepositoryTestSupport {

  @Autowired
  private UserRepository userRepository;

  @Test
  @DisplayName("계정 잠금 상태 오름차순 정렬은 잠금 계정을 먼저 조회한다.")
  void find_users_success_with_is_locked_ascending_sort() {
    // Given
    User unlocked = saveUser("unlocked@example.com", "unlocked", false);
    User locked = saveUser("locked@example.com", "locked", true);

    // When
    List<User> result = userRepository.findUsers(
        null,
        UserSortBy.isLocked,
        SortDirection.ASCENDING,
        null,
        null,
        10
    );

    // Then
    assertThat(result)
        .extracting(User::getId)
        .containsExactly(locked.getId(), unlocked.getId());
  }

  @Test
  @DisplayName("계정 잠금 상태 내림차순 정렬은 미잠금 계정을 먼저 조회한다.")
  void find_users_success_with_is_locked_descending_sort() {
    // Given
    User unlocked = saveUser("unlocked@example.com", "unlocked", false);
    User locked = saveUser("locked@example.com", "locked", true);

    // When
    List<User> result = userRepository.findUsers(
        null,
        UserSortBy.isLocked,
        SortDirection.DESCENDING,
        null,
        null,
        10
    );

    // Then
    assertThat(result)
        .extracting(User::getId)
        .containsExactly(unlocked.getId(), locked.getId());
  }

  @Test
  @DisplayName("동일한 이름으로 정렬된 사용자 목록은 ID 보조 커서 이후부터 조회한다.")
  void find_users_success_with_id_tie_breaker_cursor() {
    // Given
    saveUser("same-1@example.com", "same", false);
    saveUser("same-2@example.com", "same", false);
    saveUser("same-3@example.com", "same", false);

    List<User> all = userRepository.findUsers(
        null,
        UserSortBy.name,
        SortDirection.ASCENDING,
        null,
        null,
        10
    );
    User cursorUser = all.get(0);

    // When
    List<User> result = userRepository.findUsers(
        null,
        UserSortBy.name,
        SortDirection.ASCENDING,
        cursorUser.getName(),
        cursorUser.getId(),
        10
    );

    // Then
    assertThat(result)
        .extracting(User::getId)
        .containsExactlyElementsOf(all.stream().skip(1).map(User::getId).toList());
  }

  @Test
  @DisplayName("계정 잠금 상태 커서는 잠금 그룹 이후 미잠금 사용자를 조회한다.")
  void find_users_success_with_is_locked_cursor() {
    // Given
    User locked = saveUser("locked@example.com", "locked", true);
    User unlocked = saveUser("unlocked@example.com", "unlocked", false);

    // When
    List<User> result = userRepository.findUsers(
        null,
        UserSortBy.isLocked,
        SortDirection.ASCENDING,
        String.valueOf(locked.isBlocked()),
        locked.getId(),
        10
    );

    // Then
    assertThat(result)
        .extracting(User::getId)
        .containsExactly(unlocked.getId());
  }

  private User saveUser(String email, String name, boolean locked) {
    User user = new User(email, "encoded-password", name, Role.USER);
    user.updateBlocked(locked);
    return userRepository.saveAndFlush(user);
  }
}
