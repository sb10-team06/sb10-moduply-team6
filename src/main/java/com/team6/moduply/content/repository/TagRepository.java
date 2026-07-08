package com.team6.moduply.content.repository;

import com.team6.moduply.content.entity.Tag;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TagRepository extends JpaRepository<Tag, UUID> {

  List<Tag> findAllByTagNameIn(Collection<String> tagNames);

  @Modifying
  @Query(value = """
      INSERT INTO tags (id, tag_name, created_at, updated_at)
      VALUES (:id, :tagName, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
      ON CONFLICT (tag_name) DO NOTHING
      """, nativeQuery = true)
  int insertIgnore(
      @Param("id") UUID id,
      @Param("tagName") String tagName
  );
}
