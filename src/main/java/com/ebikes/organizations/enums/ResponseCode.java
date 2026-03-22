package com.ebikes.organizations.enums;

import static com.ebikes.organizations.constants.EventConstants.EventSource.HOST_SERVICE;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResponseCode {
  DUPLICATE_RESOURCE("DUPLICATE_RESOURCE", "Duplicate resource detected.", HttpStatus.CONFLICT),
  FORBIDDEN(
      "FORBIDDEN", "You do not have permission to perform this operation.", HttpStatus.FORBIDDEN),
  INSUFFICIENT_SCOPE(
      "INSUFFICIENT_SCOPE",
      "The token does not have the required scope to perform this operation.",
      HttpStatus.FORBIDDEN),
  INTERNAL_SERVER_ERROR(
      "INTERNAL_SERVER_ERROR",
      "An unexpected error occurred while processing your request. Please try again or contact"
          + " support.",
      HttpStatus.INTERNAL_SERVER_ERROR),
  INVALID_ARGUMENTS(
      "INVALID_ARGUMENTS",
      "Request contains invalid or incomplete arguments.",
      HttpStatus.BAD_REQUEST),
  INVALID_FORMAT("INVALID_FORMAT", "Field format is invalid.", HttpStatus.BAD_REQUEST),
  INVALID_STATE(
      "INVALID_STATE",
      "The resource is in an invalid state for the requested operation.",
      HttpStatus.BAD_REQUEST),
  MISSING_REQUIRED_FIELD(
      "MISSING_REQUIRED_FIELD",
      "Required field is missing in the request.",
      HttpStatus.BAD_REQUEST),
  RESOURCE_NOT_FOUND(
      "RESOURCE_NOT_FOUND", "The specified resource does not exist.", HttpStatus.NOT_FOUND),

  INVALID_SECURITY_CODE(
      HOST_SERVICE + ".INVALID_SECURITY_CODE",
      "The provided code is invalid or malformed.",
      HttpStatus.BAD_REQUEST),
  SECURITY_CODE_ALREADY_USED(
      HOST_SERVICE + ".SECURITY_CODE_ALREADY_USED",
      "This code has already been used.",
      HttpStatus.BAD_REQUEST),
  SECURITY_CODE_EXPIRED(
      HOST_SERVICE + ".SECURITY_CODE_EXPIRED",
      "The code has expired. Please request a new one.",
      HttpStatus.BAD_REQUEST),
  SECURITY_CODE_NOT_FOUND(
      HOST_SERVICE + ".SECURITY_CODE_NOT_FOUND",
      "The specified code does not exist.",
      HttpStatus.NOT_FOUND),
  SECURITY_CODE_TYPE_MISMATCH(
      HOST_SERVICE + ".SECURITY_CODE_TYPE_MISMATCH",
      "The code type does not match the expected type.",
      HttpStatus.BAD_REQUEST),
  TEMPLATE_PROCESSING_ERROR(
      HOST_SERVICE + ".TEMPLATE_PROCESSING_ERROR",
      "An error occurred while processing the template.",
      HttpStatus.INTERNAL_SERVER_ERROR);

  private final String code;
  private final String userMessage;
  private final HttpStatus httpStatus;
}
