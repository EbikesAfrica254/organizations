package com.ebikes.organizations.dtos.responses.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SuccessResponse<T>(String code, T data, String message) {

  public static <T> SuccessResponse<T> of(T data) {
    return new SuccessResponse<>("SUCCESS", data, null);
  }

  public static <T> SuccessResponse<T> of(T data, String message) {
    return new SuccessResponse<>("SUCCESS", data, message);
  }
}
