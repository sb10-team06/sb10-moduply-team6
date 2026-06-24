package com.team6.moduply;

import com.team6.moduply.config.TestS3Config;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestS3Config.class)
class Sb10ModuplyTeam6ApplicationTests {

  @Test
  void contextLoads() {
  }

}
