package com.team6.moduply.content.repository;

import com.team6.moduply.content.entity.ContentTag;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentTagRepository extends JpaRepository<ContentTag, UUID> {

  @Query("""
      select ct.tag.tagName
      from ContentTag ct
      where ct.content.id = :contentId
      order by ct.tag.tagName asc
      """)
  List<String> findTagNamesByContentId(@Param("contentId") UUID contentId);

  @Query("""
      select ct.content.id as contentId,
             ct.tag.tagName as tagName
      from ContentTag ct
      where ct.content.id in :contentIds
      order by ct.tag.tagName asc
      """)
  List<ContentTagNameProjection> findTagNamesByContentIds(@Param("contentIds") Collection<UUID> contentIds);

  interface ContentTagNameProjection {

    UUID getContentId();

    String getTagName();
  }
}
