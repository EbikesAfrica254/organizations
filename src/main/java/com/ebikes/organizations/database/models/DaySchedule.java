package com.ebikes.organizations.database.models;

import java.io.Serializable;
import java.time.DayOfWeek;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record DaySchedule(
    @NotNull @Pattern(
            regexp = "^([01]\\d|2[0-3]):[0-5]\\d$",
            message = "Must be in HH:mm format (24-hour)")
        String closes,
    @NotNull DayOfWeek dayOfWeek,
    @NotNull @Pattern(
            regexp = "^([01]\\d|2[0-3]):[0-5]\\d$",
            message = "Must be in HH:mm format (24-hour)")
        String opens)
    implements Serializable {

  @JsonCreator
  public DaySchedule(
      @JsonProperty("closes") String closes,
      @JsonProperty("dayOfWeek") DayOfWeek dayOfWeek,
      @JsonProperty("opens") String opens) {
    this.closes = closes;
    this.dayOfWeek = dayOfWeek;
    this.opens = opens;
  }
}
