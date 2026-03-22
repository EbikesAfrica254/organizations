package com.ebikes.organizations.exceptions;

import java.io.Serial;

import com.ebikes.organizations.enums.ResponseCode;

import lombok.Getter;

@Getter
public abstract class BaseException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  private final ResponseCode responseCode;

  protected BaseException(ResponseCode responseCode, String developerMessage) {
    super(developerMessage);
    this.responseCode = responseCode;
  }

  protected BaseException(ResponseCode responseCode, String developerMessage, Throwable cause) {
    super(developerMessage, cause);
    this.responseCode = responseCode;
  }
}
