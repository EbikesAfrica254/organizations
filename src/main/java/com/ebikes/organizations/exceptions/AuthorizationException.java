package com.ebikes.organizations.exceptions;

import java.io.Serial;

import com.ebikes.organizations.enums.ResponseCode;

public class AuthorizationException extends BaseException {

  @Serial private static final long serialVersionUID = 1L;

  public AuthorizationException(ResponseCode code, String developerMessage) {
    super(code, developerMessage);
  }
}
