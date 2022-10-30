package com.upgrade.volcanocampsitereservation.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "campsite")
@Configuration
@Getter
@Setter
public class ApplicationConfiguration {
  private long maxReservedDays;
  private long minDaysAheadOfArrival;
  private long reservationMaxDaysInAdvance;
}
