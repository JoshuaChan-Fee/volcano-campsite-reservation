package com.upgrade.volcanocampsitereservation.utils;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class Utils {
  private Utils() {
  }

  /**
   * Returns a list of dates between startInclusive and endExclusive or an empty list.
   * <p>Example:</p>
   * <pre>datesBetween('2022-10-22', '2022-10-25') returns:
   *  - '2022-10-22'
   *  - '2022-10-23'
   *  - '2022-10-24'</pre>
   *
   * @return a list of dates.
   */
  public static List<LocalDate> getDatesBetween(LocalDate startInclusive, LocalDate endExclusive) {
    if (startInclusive.isAfter(endExclusive)) {
      return List.of();
    }
    return startInclusive.datesUntil(endExclusive).collect(Collectors.toList());
  }
}
