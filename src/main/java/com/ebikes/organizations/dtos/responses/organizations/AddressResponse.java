package com.ebikes.organizations.dtos.responses.organizations;

import java.io.Serializable;
import java.math.BigDecimal;

import com.ebikes.organizations.enums.AddressTag;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AddressResponse(
    AddressTag addressTag,
    String city,
    String country,
    BigDecimal latitude,
    BigDecimal longitude,
    String postalCode,
    String streetAddress)
    implements Serializable {}
