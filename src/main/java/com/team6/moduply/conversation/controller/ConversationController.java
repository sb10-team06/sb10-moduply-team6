package com.team6.moduply.conversation.controller;

import com.team6.moduply.common.pagination.CursorResponse;
import com.team6.moduply.conversation.dto.ConversationCreateRequest;
import com.team6.moduply.conversation.dto.ConversationDto;
import com.team6.moduply.conversation.dto.ConversationFindAllRequest;
import com.team6.moduply.conversation.service.ConversationService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conversations")
public class ConversationController implements ConversationApi {

  private final ConversationService conversationService;

  @PostMapping
  @Override
  public ResponseEntity<ConversationDto> createConversation(
      @RequestBody @Valid ConversationCreateRequest request,
      @AuthenticationPrincipal(expression = "userDto.id") UUID currentUserId
  ) {
    return ResponseEntity.status(201).body(conversationService.create(request, currentUserId));
  }

  @GetMapping("/with")
  @Override
  public ResponseEntity<ConversationDto> findConversationWithUser(
      @RequestParam("userId") UUID userId,
      @AuthenticationPrincipal(expression = "userDto.id") UUID currentUserId
  ) {
    return ResponseEntity.ok(conversationService.findByUserId(userId, currentUserId));
  }

  @GetMapping("/{conversationId}")
  @Override
  public ResponseEntity<ConversationDto> findConversationById(
      @PathVariable UUID conversationId,
      @AuthenticationPrincipal(expression = "userDto.id") UUID currentUserId
  ) {
    return ResponseEntity.ok(conversationService.findById(conversationId, currentUserId));
  }

  @GetMapping
  @Override
  public ResponseEntity<CursorResponse<ConversationDto>> findConversations(
      @ModelAttribute @Valid ConversationFindAllRequest request,
      @AuthenticationPrincipal(expression = "userDto.id") UUID currentUserId
  ) {
    return ResponseEntity.ok(conversationService.findAll(request, currentUserId));
  }

  @PostMapping("/{conversationId}/direct-messages/{directMessageId}/read")
  @Override
  public ResponseEntity<Void> read(
      @PathVariable UUID conversationId,
      @PathVariable UUID directMessageId,
      @AuthenticationPrincipal(expression = "userDto.id") UUID currentUserId
  ) {
    conversationService.read(conversationId, directMessageId, currentUserId);
    return ResponseEntity.ok().build();
  }
}
