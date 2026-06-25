package com.team6.moduply.content.repository.qdsl;

import com.team6.moduply.common.pagination.SortDirection;
import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.enums.ContentSortBy;
import com.team6.moduply.content.enums.ContentType;
import java.util.List;
import java.util.UUID;

public interface ContentQDSLRepository {

  List<Content> findContents(
      ContentType typeEqual,
      String keywordLike,
      List<String> tagsIn,
      String cursor,
      UUID idAfter,
      int limit,
      ContentSortBy sortBy,
      SortDirection sortDirection
  );

  long countContents(
      ContentType typeEqual,
      String keywordLike,
      List<String> tagsIn
  );
}
