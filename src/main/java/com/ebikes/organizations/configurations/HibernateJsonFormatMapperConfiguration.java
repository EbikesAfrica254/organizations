package com.ebikes.organizations.configurations;

import java.lang.reflect.Type;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.AbstractJsonFormatMapper;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.json.JsonMapper;

/**
 * Bridges Hibernate's JSON FormatMapper to Jackson 3.
 *
 * <p>Hibernate 7's built-in {@code JacksonJsonFormatMapper} targets Jackson 2 ({@code
 * com.fasterxml.jackson}). Jackson 3 renamed its packages to {@code tools.jackson}, breaking
 * Hibernate's classpath detection for {@code @JdbcTypeCode(SqlTypes.JSON)} columns.
 *
 * <p>Remove this class once Spring Boot autoconfigures this bridge natively.
 *
 * @see <a href="https://github.com/spring-projects/spring-boot/issues/33870">spring-boot#33870</a>
 */
public final class HibernateJsonFormatMapperConfiguration extends AbstractJsonFormatMapper {

  private final JsonMapper jsonMapper = JsonMapper.builder().build();

  @Override
  public <T> void writeToTarget(
      T value, JavaType<T> javaType, Object target, WrapperOptions options) {
    jsonMapper
        .writerFor(jsonMapper.constructType(javaType.getJavaType()))
        .writeValue((JsonGenerator) target, value);
  }

  @Override
  public <T> T readFromSource(JavaType<T> javaType, Object source, WrapperOptions options) {
    return jsonMapper.readValue(
        (JsonParser) source, jsonMapper.constructType(javaType.getJavaType()));
  }

  @Override
  public boolean supportsSourceType(Class<?> sourceType) {
    return JsonParser.class.isAssignableFrom(sourceType);
  }

  @Override
  public boolean supportsTargetType(Class<?> targetType) {
    return JsonGenerator.class.isAssignableFrom(targetType);
  }

  @Override
  public <T> T fromString(CharSequence charSequence, Type type) {
    return jsonMapper.readValue(charSequence.toString(), jsonMapper.constructType(type));
  }

  @Override
  public String toString(Object value, Type type) {
    return jsonMapper.writerFor(jsonMapper.constructType(type)).writeValueAsString(value);
  }
}
