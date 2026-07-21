package com.team6.moduply.content.repository;

import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.repository.qdsl.ContentQDSLRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentRepository extends JpaRepository<Content, UUID>, ContentQDSLRepository {

  List<Content> findAllByExternalApiIdIn(Collection<String> externalApiIds);

  @EntityGraph(attributePaths = "contentImg")
  List<Content> findAllByIdIn(Collection<UUID> ids);

  @EntityGraph(attributePaths = "contentImg")
  @Query("select c from Content c where c.id = :id")
  Optional<Content> findByIdWithContentImg(@Param("id") UUID id);
}
