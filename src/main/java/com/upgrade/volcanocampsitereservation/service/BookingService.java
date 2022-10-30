package com.upgrade.volcanocampsitereservation.service;

import com.google.common.annotations.VisibleForTesting;
import com.upgrade.volcanocampsitereservation.dto.BookingDateDto;
import com.upgrade.volcanocampsitereservation.dto.BookingDto;
import com.upgrade.volcanocampsitereservation.exception.BookingConflictException;
import com.upgrade.volcanocampsitereservation.repository.BookingDateRepository;
import com.upgrade.volcanocampsitereservation.repository.BookingRepository;
import com.upgrade.volcanocampsitereservation.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class BookingService {

  private final BookingRepository bookingRepository;

  private final BookingDateRepository bookingDateRepository;

  public BookingService(BookingRepository bookingRepository, BookingDateRepository bookingDateRepository) {
    this.bookingRepository = bookingRepository;
    this.bookingDateRepository = bookingDateRepository;
  }

  @Transactional(isolation = Isolation.SERIALIZABLE)
  public BookingDto add(BookingDto bookingDto) {
    log.info("Adding {}", bookingDto);
    return addBooking(bookingDto);
  }

  private BookingDto addBooking(BookingDto bookingDto) {
    // Get dates that can be reserved by other bookings between the arrival and departure dates
    // Could throw CannotAcquireLockException
    final var bookingDatesBetween = bookingDateRepository.findAllDatesBetween(
        bookingDto.getArrivalDate(), bookingDto.getDepartureDate());
    final var bookingDates = convertBookingDateStreamToList(bookingDatesBetween);

    if (!bookingDates.isEmpty()) {
      throw new BookingConflictException("Dates " + bookingDates + " are not available");
    }
    // Save booking dates if they are available
    return saveBooking(bookingDto);
  }

  private BookingDto saveBooking(BookingDto bookingDto) {
    log.info("Saving {}", bookingDto);
    testArtificialDelay();
    // Could throw DataIntegrityViolationException (primary key constraint)
    bookingDateRepository.saveAll(bookingDto.bookingDates());
    // Could fail because of version update ObjectOptimisticLockingFailureException
    final var addedBookingDto = bookingRepository.save(bookingDto);
    log.info("Added {}", addedBookingDto);
    return addedBookingDto;
  }

  @Transactional(isolation = Isolation.SERIALIZABLE)
  public BookingDto update(BookingDto oldBookingDto, BookingDto newBookingDto) {
    log.info("Updating {} with {}", oldBookingDto, newBookingDto);
    newBookingDto.setId(oldBookingDto.getId());
    newBookingDto.setVersion(oldBookingDto.getVersion());
    // Delete the booking dates of the booking that will be modified so that the dates become available
    bookingDateRepository.deleteAll(oldBookingDto.bookingDates());
    return addBooking(newBookingDto);
  }

  @Transactional(readOnly = true)
  public Optional<BookingDto> findById(long id) {
    log.info("Find booking with id {}", id);
    return bookingRepository.findById(id);
  }

  @Transactional
  public void deleteById(long id) {
    log.info("Deleting booking with id {}", id);
    findById(id).ifPresent(booking ->
        bookingDateRepository.deleteAll(booking.bookingDates()));
    bookingRepository.deleteById(id);
  }

  @Transactional(readOnly = true)
  public List<BookingDto> findAll() {
    log.info("Find all booking");
    return bookingRepository.findAll(Sort.by("arrivalDate"));
  }

  @Transactional(readOnly = true)
  public List<LocalDate> getAvailabilities(LocalDate startInclusive, LocalDate endExclusive) {
    log.info("Get availabilities between {} and {}", startInclusive, endExclusive);
    final var availableDates = Utils.getDatesBetween(startInclusive, endExclusive);
    final var reservedDates = convertBookingDateStreamToList(
        bookingDateRepository.quickFindAllDatesBetween(startInclusive, endExclusive));
    availableDates.removeAll(reservedDates);
    return availableDates;
  }

  @VisibleForTesting
  void testArtificialDelay() {
    // This method will be used for unit test to insert an artificial delay for concurrency testing.
  }

  private List<LocalDate> convertBookingDateStreamToList(Stream<BookingDateDto> bookingDateStream) {
    return bookingDateStream
        .map(BookingDateDto::getDate)
        .collect(Collectors.toList());
  }
}
