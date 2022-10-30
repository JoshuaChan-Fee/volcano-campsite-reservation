package com.upgrade.volcanocampsitereservation.mock;

import com.upgrade.volcanocampsitereservation.domain.Booking;
import com.upgrade.volcanocampsitereservation.dto.BookingDateDto;
import com.upgrade.volcanocampsitereservation.dto.BookingDto;

import java.time.LocalDate;
import java.util.stream.Stream;

public class MockUtils {
  public static BookingDto createBookingDto(LocalDate arrivalDate, int numberOfDays) {
    return BookingDto.builder()
        .email("test@email.com")
        .fullName("Test")
        .arrivalDate(arrivalDate)
        .departureDate(arrivalDate.plusDays(numberOfDays))
        .build();
  }

  public static BookingDto createBookingDtoWithId(LocalDate arrivalDate, int numberOfDays) {
    final var bookingDto = createBookingDto(arrivalDate, numberOfDays);
    bookingDto.setId(1);
    bookingDto.setVersion(0);
    return bookingDto;
  }

  public static BookingDto createBookingDtoWithId() {
    return createBookingDtoWithId(LocalDate.now().plusDays(2), 2);
  }

  public static BookingDto createBookingDto() {
    return createBookingDto(LocalDate.now().plusDays(2), 2);
  }

  public static BookingDto createAnotherBookingDto() {
    return createBookingDto(LocalDate.now().plusDays(5), 2);
  }

  public static Booking createBooking(LocalDate arrivalDate, int numberOfDays) {
    return Booking.builder()
        .email("name@email.com")
        .fullName("name")
        .arrivalDate(arrivalDate)
        .departureDate(arrivalDate.plusDays(numberOfDays))
        .build();
  }

  public static Booking createValidBooking() {
    return createBooking(LocalDate.now().plusDays(2), 3);
  }

  public static Booking createBookingTooEarly() {
    return createBooking(LocalDate.now(), 2);
  }

  public static Booking createBookingTooLate() {
    return createBooking(LocalDate.now().plusDays(32), 1);
  }

  public static Booking createTooLongBooking() {
    return createBooking(LocalDate.now().plusDays(2), 4);
  }

  public static Booking createTooShortBooking() {
    return createBooking(LocalDate.now().plusDays(2), 0);
  }

  public static Booking createBookingWithDepartureBeforeArrival() {
    return createBooking(LocalDate.now().plusDays(2), -1);
  }

  public static Stream<BookingDateDto> bookingDatesDto(String startInclusive, int numberOfDays) {
    final var start = LocalDate.parse(startInclusive);
    final var end = start.plusDays(numberOfDays);
    return start.datesUntil(end)
        .map(date -> BookingDateDto.builder().date(date).build());
  }
}
