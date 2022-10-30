package com.upgrade.volcanocampsitereservation.controller;

import com.upgrade.volcanocampsitereservation.domain.Booking;
import com.upgrade.volcanocampsitereservation.dto.BookingDto;
import com.upgrade.volcanocampsitereservation.service.BookingService;
import com.upgrade.volcanocampsitereservation.exception.BookingConflictException;
import com.upgrade.volcanocampsitereservation.exception.BadRequestException;
import com.upgrade.volcanocampsitereservation.exception.BookingNotFoundException;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

@RestController
@Slf4j
@OpenAPIDefinition(
    info = @Info(
        title = "Volcano campsite reservation",
        description = "Volcano campsite reservation REST API service",
        contact = @Contact(email = "joshuachanfee@gmail.com", name = "Joshua Chan-Fee")))
@Tag(name = "Volcano Campsite reservation REST api")
@AllArgsConstructor
public class BookingController {

  @SuppressWarnings("java:S1075")
  static final String BASE_BOOKING_PATH = "/bookings";

  @SuppressWarnings("java:S1075")
  static final String BASE_AVAILABLE_PATH = "/availabilities";

  private final BookingService bookingService;

  @Operation(summary = "Get the list of all booked reservations.")
  @GetMapping(path = BASE_BOOKING_PATH)
  public Stream<Booking> getAllBookings() {
    return bookingService.findAll()
        .stream()
        .map(Booking::createFrom);
  }

  @Operation(summary = "Get the information of a specific reservation with a given id.")
  @GetMapping(path = BASE_BOOKING_PATH + "/{id}")
  public Booking getBooking(@PathVariable long id) {
    return bookingService.findById(id)
        .map(Booking::createFrom)
        .orElseThrow(() -> new BookingNotFoundException(id));
  }

  @Operation(summary = "Get the campsite availability days within a given date range with the default" +
          " being set to 1 month from current date.")
  @GetMapping(path = BASE_AVAILABLE_PATH)
  public List<LocalDate> getBookingAvailabilities(@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                  @RequestParam(required = false)
                                                  @Parameter(name = "Start date",
                                                          description = "Start date is included, default is set to today")
                                                  LocalDate startDate,
                                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                  @RequestParam(required = false)
                                                  @Parameter(name = "End date",
                                                          description = "End date is excluded, default is start date + 1 month")
                                                  LocalDate endDate) {
    if (startDate == null) {
      startDate = LocalDate.now();
    }
    if (endDate == null) {
      endDate = startDate.plusMonths(1);
    }
    if (startDate.isAfter(endDate)) {
      throw new BadRequestException(
              MessageFormat.format("Start date {0} is after end date {1}", startDate, endDate));
    }
    log.info("Get availabilities between {} and {}", startDate, endDate);
    return bookingService.getAvailabilities(startDate, endDate);
  }

  @Operation(summary = "Reserve the campsite.")
  @PostMapping(path = BASE_BOOKING_PATH)
  public Booking addBooking(@Valid @RequestBody Booking booking) {
    try {
      log.info("Add booking {}", booking);
      return Booking.createFrom(bookingService.add(BookingDto.createFrom(booking)));
    } catch (DataAccessException e) {
      throw new BookingConflictException("Selected dates are not available to be reserved");
    }
  }



  @Operation(summary = "Update booking reservation with the given id.")
  @PutMapping(path = BASE_BOOKING_PATH + "/{id}")
  public Booking updateBooking(@PathVariable long id, @Valid @RequestBody Booking booking) {
    log.info("Update booking {} with {}", id, booking);
    final var oldBookingDto = bookingService.findById(id)
        .orElseThrow(() -> new BookingNotFoundException(id));
    final var newBookingDto = BookingDto.createFrom(booking);
    return Booking.createFrom(bookingService.update(oldBookingDto, newBookingDto));
  }

  @Operation(summary = "Cancel booking reservation with the given id.")
  @DeleteMapping(path = BASE_BOOKING_PATH + "/{id}")
  public void deleteBooking(@PathVariable long id) {
    log.info("Delete booking {}", id);
    try {
      bookingService.deleteById(id);
    } catch (EmptyResultDataAccessException e) {
      throw new BookingNotFoundException(id);
    }
  }
}
