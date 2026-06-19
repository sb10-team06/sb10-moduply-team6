package com.team6.moduply.config.support;

import com.team6.moduply.config.TestcontainersConfig;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@Import(TestcontainersConfig.class)
@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
public abstract class IntegrationTestSupport {

}
