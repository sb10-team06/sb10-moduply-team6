package com.team6.moduply.notification.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
  ROLE_CHANGED("[권한 변경]", "내 권한이 [%s]에서 [%s]로 변경되었어요"),
  PLAYLIST_SUBSCRIBED("[구독]", "%s님이 '%s' 플레이리스트를 구독했어요."),
  CONTENT_ADDED("[새 콘텐츠 추가]", "'%s' 플레이리스트에 '%s' 콘텐츠가 추가되었어요."),
  FOLLOW_ACTIVITY("[팔로우 활동]", "%s님이 %s"),
  FOLLOWED("[팔로우]", "%s님이 나를 팔로우했어요."),
  DM_RECEIVED("[DM] %s", "%s");

  private final String title;
  private final String messageTemplate;
}
