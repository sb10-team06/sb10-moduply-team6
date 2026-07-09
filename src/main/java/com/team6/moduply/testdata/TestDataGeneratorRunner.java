package com.team6.moduply.testdata;

import com.team6.moduply.content.entity.Content;
import com.team6.moduply.testdata.generator.ContentGenerator;
import com.team6.moduply.testdata.generator.UserGenerator;
import com.team6.moduply.user.entity.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("data-gen")
@Order(2)
@RequiredArgsConstructor
public class TestDataGeneratorRunner implements CommandLineRunner {

  private final ApplicationContext context;
  private final UserGenerator userGenerator;
  private final ContentGenerator contentGenerator;

  @Override
  public void run(String... args) {
    log.info("데이터 생성 시작");

    // 데이터 생성 순서 주의(의존 관계 고려)
    userGenerator.generate();
    log.info("사용자 데이터 생성 완료");

    contentGenerator.generate();
    log.info("콘텐츠 데이터 생성 완료");

    SpringApplication.exit(context, () -> 0);
  }
}
