package com.team6.moduply.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.team6.moduply.binarycontent.entity.BinaryContent;
import com.team6.moduply.user.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTest {

  @Test
  @DisplayName("프로필 이미지를 변경하면 바이너리 콘텐츠와 이미지 URL이 함께 갱신된다.")
  void updateProfileImage_success_with_url() {
    User user = new User("user@example.com", "encoded-password", "사용자", Role.USER);
    BinaryContent profileImg = BinaryContent.create(
        "profile.png",
        1024L,
        "image/png",
        "users/user-id/profile/profile.png"
    );
    String profileImageUrl =
        "https://bucket.s3.ap-northeast-2.amazonaws.com/users/user-id/profile/profile.png";

    user.updateProfileImage(profileImg, profileImageUrl);

    assertThat(user.getProfileImg()).isSameAs(profileImg);
    assertThat(user.getProfileImageUrl()).isEqualTo(profileImageUrl);
  }

  @Test
  @DisplayName("프로필 이미지를 다시 변경하면 바이너리 콘텐츠와 이미지 URL이 새 값으로 함께 갱신된다.")
  void updateProfileImage_success_with_replacement() {
    User user = new User("user@example.com", "encoded-password", "사용자", Role.USER);
    BinaryContent oldProfileImg = BinaryContent.create(
        "old-profile.png",
        1024L,
        "image/png",
        "users/user-id/profile/old-profile.png"
    );
    BinaryContent newProfileImg = BinaryContent.create(
        "new-profile.png",
        2048L,
        "image/png",
        "users/user-id/profile/new-profile.png"
    );
    user.updateProfileImage(oldProfileImg, "https://bucket/old-profile.png");

    user.updateProfileImage(newProfileImg, "https://bucket/new-profile.png");

    assertThat(user.getProfileImg()).isSameAs(newProfileImg);
    assertThat(user.getProfileImageUrl()).isEqualTo("https://bucket/new-profile.png");
  }

  @SuppressWarnings("deprecation")
  @Test
  @DisplayName("기존 프로필 이미지 변경 메서드를 사용하면 이전 이미지 URL이 제거된다.")
  void updateProfileImg_success_clears_url() {
    User user = new User("user@example.com", "encoded-password", "사용자", Role.USER);
    BinaryContent oldProfileImg = BinaryContent.create(
        "old-profile.png",
        1024L,
        "image/png",
        "users/user-id/profile/old-profile.png"
    );
    BinaryContent newProfileImg = BinaryContent.create(
        "new-profile.png",
        2048L,
        "image/png",
        "users/user-id/profile/new-profile.png"
    );
    user.updateProfileImage(oldProfileImg, "https://bucket/old-profile.png");

    user.updateProfileImg(newProfileImg);

    assertThat(user.getProfileImg()).isSameAs(newProfileImg);
    assertThat(user.getProfileImageUrl()).isNull();
  }
}
