package com.team6.moduply.notification.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
  ROLE_CHANGED("권한 변경 알림", "회원님의 권한이 변경되었습니다."),           // 권한 변경
  PLAYLIST_SUBSCRIBED("플레이리스트 구독 알림", "'%s' 플레이리스트를 누군가 구독했습니다."),
  CONTENT_ADDED("새 콘텐츠 추가 알림", "'%s' 플레이리스트에 '%s' 콘텐츠가 추가되었습니다."),
  FOLLOW_ACTIVITY("팔로우 활동 알림", "%s님이 %s"),
  FOLLOWED("팔로우 알림", "%s님이 회원님을 팔로우했습니다."),
  DM_RECEIVED("DM 수신 알림", "%s님에게 새로운 메시지가 도착했습니다.");

  private final String title;
  private final String messageTemplate;
}
