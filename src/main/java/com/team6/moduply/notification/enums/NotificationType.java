package com.team6.moduply.notification.enums;

public enum NotificationType {
  ROLE_CHANGED,           // 권한 변경
  PLAYLIST_SUBSCRIBED,    // 내 플레이리스트를 구독
  CONTENT_ADDED,          // 구독 중인 플레이리스트에 콘텐츠 추가
  FOLLOW_ACTIVITY,        // 팔로우한 사용자의 주요 활동
  FOLLOWED,               // 다른 사용자가 나를 팔로우
  DM_RECEIVED             // DM 수신
}
