package com.ebikes.organizations.support.infrastructure;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class PostgresContainerConfig {

  private static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:17-alpine")
          .withDatabaseName("ebikes_organizations_test")
          .withUsername("test")
          .withPassword("test")
          .withInitScript("init.sql");

  static {
    POSTGRES.start();
  }

  @Bean
  @ServiceConnection(name = "postgresql")
  public PostgreSQLContainer postgresContainer() {
    return POSTGRES;
  }
}
