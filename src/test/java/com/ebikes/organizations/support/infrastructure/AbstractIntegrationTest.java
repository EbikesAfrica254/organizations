package com.ebikes.organizations.support.infrastructure;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles({"test", "integration-test"})
@AutoConfigureMockMvc
@ExtendWith(WithExecutionContext.class)
@Import({IntegrationContainersConfig.class})
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Tag("integration")
public abstract class AbstractIntegrationTest {

  @MockitoBean protected JwtDecoder jwtDecoder;
}
