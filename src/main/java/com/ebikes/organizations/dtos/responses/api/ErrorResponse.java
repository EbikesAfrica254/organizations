package com.ebikes.organizations.dtos.responses.api;

import static com.ebikes.organizations.constants.ApplicationConstants.DOCUMENTATION_ERRORS_BASE;

import java.util.List;

import com.ebikes.organizations.enums.ResponseCode;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    String code,
    String detail,
    String errorReference,
    List<ErrorDetail> errors,
    String instance,
    int status,
    String title,
    String type) {

  public static ErrorResponse from(
      String detail, String errorReference, String instance, ResponseCode responseCode) {
    return new ErrorResponse(
        responseCode.getCode(),
        detail,
        errorReference,
        null,
        instance,
        responseCode.getHttpStatus().value(),
        responseCode.getUserMessage(),
        buildTypeUri(responseCode));
  }

  public static ErrorResponse withErrors(
      String detail,
      String errorReference,
      List<ErrorDetail> errors,
      String instance,
      ResponseCode responseCode) {
    return new ErrorResponse(
        responseCode.getCode(),
        detail,
        errorReference,
        errors,
        instance,
        responseCode.getHttpStatus().value(),
        responseCode.getUserMessage(),
        buildTypeUri(responseCode));
  }

  private static String buildTypeUri(ResponseCode responseCode) {
    String encodedCode = responseCode.getCode().toLowerCase().replace('_', '-');
    return DOCUMENTATION_ERRORS_BASE + encodedCode;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record ErrorDetail(String field, String message, Object rejectedValue) {

    public static ErrorDetail of(String field, String message) {
      return new ErrorDetail(field, message, null);
    }

    public static ErrorDetail of(String field, String message, Object rejectedValue) {
      return new ErrorDetail(field, message, rejectedValue);
    }
  }
}
