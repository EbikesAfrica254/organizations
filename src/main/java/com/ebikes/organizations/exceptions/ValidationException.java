package com.ebikes.organizations.exceptions;

import java.io.Serial;
import java.io.Serializable;

import com.ebikes.organizations.enums.ResponseCode;

import lombok.Getter;

@Getter
public class ValidationException extends BaseException {

  @Serial private static final long serialVersionUID = 1L;

  private final Serializable rejectedValue;
  private final String field;

  public ValidationException(
      ResponseCode code, String developerMessage, String field, Serializable rejectedValue) {
    super(code, developerMessage);
    this.field = field;
    this.rejectedValue = rejectedValue;
  }
}
