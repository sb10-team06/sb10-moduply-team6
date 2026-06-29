package com.team6.moduply.common.util;

import com.team6.moduply.auth.exception.AuthErrorCode;
import com.team6.moduply.auth.exception.AuthException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TempPasswordUtil {
  // 암호학적으로 안전한 난수 생성기 (해킹 예측 불가)
  private static final SecureRandom RANDOM = new SecureRandom();

  // 문자열 풀(Pool) 세팅
  private static final String LOWER_CASE = "abcdefghijklmnopqrstuvwxyz";
  private static final String UPPER_CASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static final String DIGITS = "0123456789";
  private static final String SPECIAL_CHARS = "!@#$%^&*()-_=+";

  // 전체 문자를 합친 풀
  private static final String ALL_CHARS = LOWER_CASE + UPPER_CASE + DIGITS + SPECIAL_CHARS;

  //영대소문자, 숫자, 특수문자가 각각 최소 1개 이상 포함된 임시 비밀번호를 생성.
  public String generate(int length) {
    if (length < 8) {
      throw new AuthException(AuthErrorCode.INVALID_TEMP_PASSWORD_EXCEPTION, Map.of(
          "length", length
      ));
    }

    List<Character> passwordChars = new ArrayList<>();

    // 각 카테고리별로 최소 1개의 문자를 무조건 뽑아서 넣음. (보안 강화)
    passwordChars.add(LOWER_CASE.charAt(RANDOM.nextInt(LOWER_CASE.length())));
    passwordChars.add(UPPER_CASE.charAt(RANDOM.nextInt(UPPER_CASE.length())));
    passwordChars.add(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
    passwordChars.add(SPECIAL_CHARS.charAt(RANDOM.nextInt(SPECIAL_CHARS.length())));

    // 남은 길이만큼 전체 문자 풀에서 랜덤하게 뽑아 채움.
    for (int i = 4; i < length; i++) {
      passwordChars.add(ALL_CHARS.charAt(RANDOM.nextInt(ALL_CHARS.length())));
    }

    // 문자들이 항상 같은 순서(소->대->숫->특)로 나오지 않도록 섞음.
    Collections.shuffle(passwordChars, RANDOM);

    // List를 String으로 변환해서 반환.
    StringBuilder password = new StringBuilder();
    for (char c : passwordChars) {
      password.append(c);
    }

    return password.toString();
  }
}
