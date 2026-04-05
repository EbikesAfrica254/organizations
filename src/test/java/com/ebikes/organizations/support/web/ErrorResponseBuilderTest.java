package com.ebikes.organizations.support.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.ebikes.organizations.constants.ApplicationConstants;
import com.ebikes.organizations.dtos.responses.api.ErrorResponse;
import com.ebikes.organizations.dtos.responses.api.ErrorResponse.ErrorDetail;
import com.ebikes.organizations.enums.ResponseCode;

@DisplayName("ErrorResponseBuilder")
class ErrorResponseBuilderTest {

  @Test
  @DisplayName("buildErrorResponse should return ErrorResponse with correct fields")
  void buildErrorResponseShouldReturnCorrectFields() {
    ErrorResponse response =
        ErrorResponseBuilder.buildErrorResponse(
            "something went wrong", "/api/test", ResponseCode.RESOURCE_NOT_FOUND);

    assertThat(response.detail()).isEqualTo("something went wrong");
    assertThat(response.instance()).isEqualTo("/api/test");
    assertThat(response.status())
        .isEqualTo(ResponseCode.RESOURCE_NOT_FOUND.getHttpStatus().value());
    assertThat(response.errorReference()).isNotBlank();
    assertThat(response.errors()).isNull();
  }

  @Test
  @DisplayName("buildErrorResponseWithErrors should return ErrorResponse with errors populated")
  void buildErrorResponseWithErrorsShouldPopulateErrors() {
    List<ErrorDetail> errors = List.of(ErrorDetail.of("field", "must not be blank"));

    ErrorResponse response =
        ErrorResponseBuilder.buildErrorResponseWithErrors(
            "validation failed", errors, "/api/test", ResponseCode.INVALID_ARGUMENTS);

    assertThat(response.detail()).isEqualTo("validation failed");
    assertThat(response.errors())
        .hasSize(1)
        .first()
        .satisfies(
            e -> {
              assertThat(e.field()).isEqualTo("field");
              assertThat(e.message()).isEqualTo("must not be blank");
            });
  }

  @Test
  @DisplayName("buildResponse should return ResponseEntity with correct status and Content-Type")
  void buildResponseShouldReturnCorrectStatusAndContentType() {
    ErrorResponse errorResponse =
        ErrorResponseBuilder.buildErrorResponse(
            "error", "/api/test", ResponseCode.RESOURCE_NOT_FOUND);

    ResponseEntity<ErrorResponse> entity =
        ErrorResponseBuilder.buildResponse(errorResponse, HttpStatus.NOT_FOUND);

    assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(entity.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE))
        .isEqualTo(ApplicationConstants.PROBLEM_JSON_MEDIA_TYPE);
    assertThat(entity.getBody()).isEqualTo(errorResponse);
  }
}
