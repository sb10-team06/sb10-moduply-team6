package com.team6.moduply.user.entity;

import com.team6.moduply.binarycontent.entity.BinaryContent;
import com.team6.moduply.common.baseentity.BaseEntity;
import com.team6.moduply.common.baseentity.BaseUpdatableEntity;
import com.team6.moduply.user.enums.Role;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class User extends BaseUpdatableEntity {
  @Column(name = "email", nullable = false, unique = true)
  private String email;

  // 소셜 로그인시 비밀번호 필요없기 때문에 nullable = true
  @Column(name = "password")
  private String encodedPassword;

  @Column(name = "name", nullable = false)
  private String name;

  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  @JoinColumn(name = "profile_img_id")
  private BinaryContent profileImg;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false)
  private Role role;

  @Column(name = "is_blocked")
  private boolean isBlocked = false;


  public User(String email, String encodedPassword, String name, Role role) {
    // 추후 커스텀 예외로 변경
    if (!StringUtils.hasText(email)) {
      throw new IllegalArgumentException("email must not be blank");
    }
    // 추후 커스텀 예외로 변경
    if (!StringUtils.hasText(name)) {
      throw new IllegalArgumentException("name must not be blank");
    }

    this.email = email;
    this.encodedPassword = encodedPassword;
    this.name = name;
    this.role = Objects.requireNonNull(role, "role must not be null");
  }

  public void block() {
    this.isBlocked = true;
  }

  public void updateName(String name){
    // 추후 커스텀 예외로 변경
    if (!StringUtils.hasText(name)) {
      throw new IllegalArgumentException("name must not be blank");
    }
    this.name = name;
  }

  public void updateEncodedPassword(String encodedPassword){
    this.encodedPassword = encodedPassword;
  }

  public void updateRole(Role role){
    this.role = Objects.requireNonNull(role, "role must not be null");
  }

  public void updateProfileImg(BinaryContent profileImg) {
    this.profileImg = profileImg;
  }
}
