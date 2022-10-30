package com.upgrade.volcanocampsitereservation.dto;

import com.upgrade.volcanocampsitereservation.domain.Booking;
import com.upgrade.volcanocampsitereservation.utils.Utils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Version;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Entity(name = "Booking")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDto {

  @Id
  @GeneratedValue
  private long id;

  @Version
  @GeneratedValue
  private long version;

  private String email;

  private String fullName;

  private LocalDate arrivalDate;

  private LocalDate departureDate;

  /**
   * Generates a BookingDto object from Booking object.
   */
  public static BookingDto createFrom(Booking booking) {
    return BookingDto.builder()
        .email(booking.getEmail())
        .fullName(booking.getFullName())
        .arrivalDate(booking.getArrivalDate())
        .departureDate(booking.getDepartureDate())
        .build();
  }

  public List<BookingDateDto> bookingDates() {
    return Utils.getDatesBetween(arrivalDate, departureDate)
        .stream()
        .map(localDate -> BookingDateDto.builder().date(localDate).build())
        .collect(Collectors.toList());
  }
}
