package com.team6.moduply.content.repository;

import com.team6.moduply.content.entity.ContentTag;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentTagRepository extends JpaRepository<ContentTag, UUID> {
}
