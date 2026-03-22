package com.ebikes.organizations;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OrganizationsApplication {

  public static void main(String[] args) {
    SpringApplication.run(OrganizationsApplication.class, args);
  }
}
