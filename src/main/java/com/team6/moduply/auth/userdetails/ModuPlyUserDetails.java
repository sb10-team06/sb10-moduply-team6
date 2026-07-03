package com.team6.moduply.auth.userdetails;

import com.team6.moduply.user.dto.UserDto;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

@Getter
public class ModuPlyUserDetails implements UserDetails, OAuth2User {
  private final UserDto userDto;
  private final String password;
  private final Map<String, Object> attributes;

  public ModuPlyUserDetails(UserDto userDto, String password) {
    this.userDto = userDto;
    this.password = password;
    this.attributes = null;
  }

  public ModuPlyUserDetails(UserDto userDto, Map<String, Object> attributes) {
    this.userDto = userDto;
    this.password = "";
    this.attributes = attributes;
  }

  @Override
  public String getName() {
    return userDto.getEmail();
  }

  @Override
  public Map<String, Object> getAttributes() {
    return this.attributes;
  }

  @Override
  public String getUsername() {
    return userDto.getEmail();
  }

  @Override
  public String getPassword() {
    return this.password;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + userDto.getRole().name()));
  }

  @Override
  public boolean isEnabled() {
    return UserDetails.super.isEnabled();
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return UserDetails.super.isCredentialsNonExpired();
  }

  @Override
  public boolean isAccountNonLocked() {
    return !userDto.isLocked();
  }

  @Override
  public boolean isAccountNonExpired() {
    return UserDetails.super.isAccountNonExpired();
  }
}
