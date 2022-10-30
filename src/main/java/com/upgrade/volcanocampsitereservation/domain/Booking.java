package com.upgrade.volcanocampsitereservation.domain;

import com.upgrade.volcanocampsitereservation.dto.BookingDto;
import com.upgrade.volcanocampsitereservation.validation.BookingGuideLine;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.Email;
import javax.validation.constraints.Future;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Value
@Builder
@BookingGuideLine
public class Booking {

  @Email(message = "Email should be valid")
  @NotBlank(message = "Email cannot be blank")
  String email;

  @NotBlank(message = "FullName cannot be blank")
  String fullName;

  @Future(message = "ArrivalDate must occur in the future")
  @NotNull(message = "ArrivalDate is a mandatory field")
  LocalDate arrivalDate;

  @Future(message = "DepartureDate must occur in the future")
  @NotNull(message = "DepartureDate is a mandatory field")
  LocalDate departureDate;

  // This returns the ID of a booking in the response
  @Schema(hidden = true)
  long id;

  /**
   * Generates a Booking object from BookingDto object.
   */
  public static Booking createFrom(BookingDto bookingDto) {
    return Booking.builder()
        .id(bookingDto.getId())
        .email(bookingDto.getEmail())
        .fullName(bookingDto.getFullName())
        .arrivalDate(bookingDto.getArrivalDate())
        .departureDate(bookingDto.getDepartureDate())
        .build();
  }

}
