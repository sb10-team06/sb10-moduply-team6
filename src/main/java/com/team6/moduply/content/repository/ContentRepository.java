package com.team6.moduply.content.repository;

import com.team6.moduply.content.entity.Content;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentRepository extends JpaRepository<Content, UUID> {
}
