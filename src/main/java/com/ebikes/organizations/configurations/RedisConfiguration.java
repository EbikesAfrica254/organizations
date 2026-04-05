package com.ebikes.organizations.configurations;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class RedisConfiguration {
  @Bean
  public RedisCacheConfiguration cacheConfiguration() {
    return RedisCacheConfiguration.defaultCacheConfig()
        .disableCachingNullValues()
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(
                GenericJacksonJsonRedisSerializer.builder().build()));
  }

  @Bean
  @ConditionalOnProperty(
      value = {"spring.data.redis.ssl.enabled"},
      havingValue = "true")
  public org.springframework.boot.data.redis.autoconfigure
          .LettuceClientConfigurationBuilderCustomizer
      lettuceClientConfigurationBuilderCustomizer() {
    return clientConfigurationBuilder -> {
      log.info("lettuceClientConfigurationBuilderCustomizer : disabling peer verification");
      clientConfigurationBuilder.useSsl().disablePeerVerification();
    };
  }
}
