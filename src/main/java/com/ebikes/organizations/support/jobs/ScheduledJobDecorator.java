package com.ebikes.organizations.support.jobs;

import com.ebikes.organizations.support.context.ExecutionContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ScheduledJobDecorator {

  private ScheduledJobDecorator() {
    // prevent instantiation
  }

  public static void decorate(Runnable job) {
    try {
      ExecutionContext.setSystem();
      job.run();
    } finally {
      ExecutionContext.clear();
    }
  }
}
