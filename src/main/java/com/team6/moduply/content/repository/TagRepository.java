package com.team6.moduply.content.repository;

import com.team6.moduply.content.entity.Tag;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, UUID> {

  List<Tag> findAllByTagNameIn(Collection<String> tagNames);
}
