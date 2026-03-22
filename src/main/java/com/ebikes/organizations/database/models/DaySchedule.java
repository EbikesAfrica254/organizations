package com.ebikes.organizations.database.models;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import jakarta.validation.constraints.AssertTrue;
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

  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

  @JsonCreator
  public DaySchedule(
      @JsonProperty("closes") String closes,
      @JsonProperty("dayOfWeek") DayOfWeek dayOfWeek,
      @JsonProperty("opens") String opens) {
    this.closes = closes;
    this.dayOfWeek = dayOfWeek;
    this.opens = opens;
  }

  @AssertTrue(message = "Opening time must be before closing time") private boolean isScheduleValid() {
    if (opens == null || closes == null) {
      return true;
    }
    return LocalTime.parse(opens, TIME_FORMAT).isBefore(LocalTime.parse(closes, TIME_FORMAT));
  }
}
