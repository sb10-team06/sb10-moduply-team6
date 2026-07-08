package com.team6.moduply.user.repository.qdsl;

import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.user.entity.User;
import com.team6.moduply.user.enums.UserSortBy;
import java.util.List;
import java.util.UUID;

public interface UserQDSLRepository {
  List<User> findUsers(String keyword, UserSortBy orderBy, SortDirection direction, String cursor, UUID idAfter, int limit);
  long countUsers(String keyword);
}
