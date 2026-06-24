package com.team6.moduply.content.service;

import com.team6.moduply.binarycontent.entity.BinaryContent;
import com.team6.moduply.content.dto.ContentCreateRequest;
import com.team6.moduply.content.dto.ContentDto;
import com.team6.moduply.content.entity.Content;
import com.team6.moduply.content.entity.ContentTag;
import com.team6.moduply.content.entity.Tag;
import com.team6.moduply.content.exception.ContentErrorCode;
import com.team6.moduply.content.exception.ContentException;
import com.team6.moduply.content.mapper.ContentMapper;
import com.team6.moduply.content.repository.ContentTagRepository;
import com.team6.moduply.content.repository.ContentRepository;
import com.team6.moduply.content.repository.TagRepository;
import com.team6.moduply.user.enums.Role;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentService {

  private final ContentRepository contentRepository;
  private final TagRepository tagRepository;
  private final ContentTagRepository contentTagRepository;
  private final ContentMapper contentMapper;

  @Transactional
  public ContentDto createContent(
      ContentCreateRequest request,
      BinaryContent contentImg,
      String thumbnailUrl,
      Role requesterRole
  ) {
    log.debug("콘텐츠 생성 처리 시작: type={}, title={}", request.type(), request.title());
    // TODO: Spring Security 인가 구조 적용 시 createContent 메서드에 관리자 권한 검증 적용 예정
    validateAdmin(requesterRole);

    Content content = new Content(
        contentImg,
        null,
        request.type(),
        request.title(),
        request.description()
    );
    Content savedContent = contentRepository.save(content);

    List<String> tagNames = normalizeTags(request.tags());

    List<Tag> tags = getOrCreateTags(tagNames);

    List<ContentTag> contentTags = tags.stream()
        .map(tag -> new ContentTag(savedContent, tag))
        .toList();

    if (!contentTags.isEmpty()) {
      contentTagRepository.saveAll(contentTags);
    }

    ContentDto response = contentMapper.toDto(savedContent, thumbnailUrl, tagNames);

    log.debug("콘텐츠 생성 처리 완료: contentId={}", savedContent.getId());

    return response;
  }

  private void validateAdmin(Role requesterRole) {
    if (requesterRole != Role.ADMIN) {
      log.warn("콘텐츠 생성 실패: 관리자 권한 없음. requesterRole={}", requesterRole);
      throw new ContentException(ContentErrorCode.CONTENT_CREATE_FORBIDDEN, Map.of(
          "requesterRole", requesterRole == null ? "null" : requesterRole.name()
      ));
    }
  }

  private List<String> normalizeTags(List<String> tags) {
    if (tags == null) {
      return List.of();
    }

    return tags.stream()
        .map(String::trim)
        .collect(Collectors.toCollection(LinkedHashSet::new))
        .stream()
        .toList();
  }

  private List<Tag> getOrCreateTags(List<String> tagNames) {
    if (tagNames.isEmpty()) {
      return List.of();
    }

    Map<String, Tag> existingTags = tagRepository.findAllByTagNameIn(tagNames).stream()
        .collect(Collectors.toMap(Tag::getTagName, Function.identity()));

    Set<String> existingTagNames = existingTags.keySet();
    List<String> newTagNames = tagNames.stream()
        .filter(tagName -> !existingTagNames.contains(tagName))
        .toList();

    if (!newTagNames.isEmpty()) {
      newTagNames.forEach(tagName -> tagRepository.insertIgnore(UUID.randomUUID(), tagName));
      existingTags = tagRepository.findAllByTagNameIn(tagNames).stream()
          .collect(Collectors.toMap(Tag::getTagName, Function.identity()));
    }

    return tagNames.stream()
        .map(existingTags::get)
        .toList();
  }
}
