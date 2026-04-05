package com.ebikes.organizations.support.infrastructure;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

@Import(PostgresContainerConfig.class)
@TestConfiguration(proxyBeanMethods = false)
public class IntegrationContainersConfig {

  private static final RabbitMQContainer RABBITMQ =
      new RabbitMQContainer("rabbitmq:4.2.4-management-alpine");

  @SuppressWarnings("resource")
  private static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);

  static {
    RABBITMQ.start();
    REDIS.start();
  }

  @Bean
  @ServiceConnection
  public RabbitMQContainer rabbitMqContainer() {
    return RABBITMQ;
  }

  @Bean
  @ServiceConnection(name = "redis")
  public GenericContainer<?> redisContainer() {
    return REDIS;
  }
}
