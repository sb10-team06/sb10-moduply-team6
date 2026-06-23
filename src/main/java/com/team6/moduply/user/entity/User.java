package com.team6.moduply.user.entity;

import com.team6.moduply.binarycontent.entity.BinaryContent;
import com.team6.moduply.common.BaseEntity;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class User extends BaseEntity {
  @Column(name = "email", nullable = false, unique = true)
  private String email;

  // 소셜 로그인시 비밀번호 필요없기 때문에 nullable = true
  @Column(name = "password")
  private String password;

  @Column(name = "name", nullable = false)
  private String name;

  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  @JoinColumn(name = "profile_img_id")
  private BinaryContent profileImg;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false)
  private Role role = Role.USER;

  @Column(name = "is_blocked")
  private boolean isBlocked = false;


  public User(String email, String password, String name, Role role) {
    this.email = email;
    this.password = password;
    this.name = name;
    this.role = role;
  }

  public void block() {
    this.isBlocked = true;
  }

  public void updateName(String name){
    this.name = name;
  }

  public void updateEncodedPassword(String encodedPassword){
    this.password = encodedPassword;
  }

  public void updateProfileImg(BinaryContent profileImg) {
    this.profileImg = profileImg;
  }
}
