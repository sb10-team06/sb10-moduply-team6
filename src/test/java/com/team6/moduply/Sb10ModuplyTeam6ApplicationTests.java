package com.team6.moduply;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.s3.S3Client;

@SpringBootTest
class Sb10ModuplyTeam6ApplicationTests {

  @MockitoBean
  private S3Client s3Client;

  @Test
  void contextLoads() {
  }

}
