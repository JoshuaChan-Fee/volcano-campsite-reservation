package com.upgrade.volcanocampsitereservation.service;

import com.upgrade.volcanocampsitereservation.mock.MockUtils;
import com.upgrade.volcanocampsitereservation.dto.BookingDto;
import com.upgrade.volcanocampsitereservation.repository.BookingDateRepository;
import com.upgrade.volcanocampsitereservation.repository.BookingRepository;
import com.upgrade.volcanocampsitereservation.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.EmptyResultDataAccessException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test the {@link BookingService}.
 * <p>This test class uses the h2 in-memory database.</p>
 */
@SpringBootTest
@Slf4j
class BookingServiceTest {

  @Autowired
  private BookingService bookingService;

  @Autowired
  private BookingRepository bookingRepository;

  @Autowired
  private BookingDateRepository bookingDateRepository;

  @BeforeEach
  void setUp() {
    bookingRepository.deleteAll();
    bookingDateRepository.deleteAll();
  }

  private BookingDto createAndAddBookingDto() {
    final var validBookingDto = MockUtils.createBookingDto();
    return bookingService.add(validBookingDto);
  }

  private BookingDto createAndAddAnotherBookingDto() {
    final var validBookingDto = MockUtils.createAnotherBookingDto();
    return bookingService.add(validBookingDto);
  }

  @Test
  void add_success() {
    final var bookingDto = MockUtils.createBookingDto();

    final var addedBookingDto = bookingService.add(bookingDto);

    assertThat(bookingRepository.findById(addedBookingDto.getId()))
        .get().usingRecursiveComparison().ignoringFields("id", "version").isEqualTo(bookingDto);
    assertThat(bookingRepository.findById(addedBookingDto.getId())).get().isEqualTo(addedBookingDto);
    assertThat(bookingDateRepository.findAll()).containsExactlyElementsOf(bookingDto.bookingDates());
  }

  @Test
  void update() {
    final var oldBookingDto = MockUtils.createBookingDto();
    final var newBookingDto = MockUtils.createAnotherBookingDto();

    final var updatedBookingDto = bookingService.update(oldBookingDto, newBookingDto);

    assertThat(bookingRepository.findById(updatedBookingDto.getId()))
        .get().usingRecursiveComparison().ignoringFields("id", "version").isEqualTo(newBookingDto);
    assertThat(bookingRepository.findById(updatedBookingDto.getId())).get().isEqualTo(updatedBookingDto);
    assertThat(bookingDateRepository.findAll()).containsExactlyElementsOf(newBookingDto.bookingDates());
  }

  @Test
  void findById_absent() {
    assertThat(bookingService.findById(0)).isNotPresent();
  }

  @Test
  void findById_success() {
    final var bookingDto = createAndAddBookingDto();

    final var optionalBookingDto = bookingService.findById(bookingDto.getId());

    assertThat(optionalBookingDto).get().isEqualTo(bookingDto);
  }

  @Test
  void deleteById_failure() {
    assertThatThrownBy(() -> bookingService.deleteById(0))
        .isInstanceOf(EmptyResultDataAccessException.class);
  }

  @Test
  void deleteById_success() {
    final var bookingDto = createAndAddBookingDto();

    bookingService.deleteById(bookingDto.getId());

    assertThat(bookingRepository.findAll()).isEmpty();
    assertThat(bookingDateRepository.findAll()).isEmpty();
  }

  @Test
  void findAll_empty() {
    assertThat(bookingService.findAll()).isEmpty();
  }

  @Test
  void findAll_1() {
    final var bookingDto = createAndAddBookingDto();

    final var bookingDtoList = bookingService.findAll();
    assertThat(bookingDtoList).containsExactly(bookingDto);
  }

  @Test
  void findAll_2() {
    final var bookingDto1 = createAndAddBookingDto();
    final var bookingDto2 = createAndAddAnotherBookingDto();

    final var bookingDtoList = bookingService.findAll();

    assertThat(bookingDtoList).containsExactly(bookingDto1, bookingDto2);
  }

  @Test
  void getAvailabilities_0booking() {
    final var startInclusive = LocalDate.now();
    final var endExclusive = startInclusive.plusDays(10);

    final var availabilities = bookingService.getAvailabilities(startInclusive, endExclusive);

    final var expectedAvailabilities = Utils.getDatesBetween(startInclusive, endExclusive);
    assertThat(availabilities).containsExactlyElementsOf(expectedAvailabilities);
  }

  @Test
  void getAvailabilities_1booking() {
    final var bookingDto = createAndAddBookingDto();
    // Get the availabilities between 5 days before and 5 days after the booking
    final var startInclusive = bookingDto.getArrivalDate().minusDays(5);
    final var endExclusive = bookingDto.getDepartureDate().plusDays(5);

    final var availabilities = bookingService.getAvailabilities(startInclusive, endExclusive);

    final var expectedAvailabilities = Utils.getDatesBetween(startInclusive, bookingDto.getArrivalDate());
    expectedAvailabilities.addAll(Utils.getDatesBetween(bookingDto.getDepartureDate(), endExclusive));
    assertThat(availabilities).containsExactlyElementsOf(expectedAvailabilities);
  }

  @Test
  void getAvailabilities_2bookings() {
    final var bookingDto1 = createAndAddBookingDto();
    final var bookingDto2 = createAndAddAnotherBookingDto();
    // Get the availabilities between the start of the booking1 to the end of the booking2
    final var startInclusive = bookingDto1.getArrivalDate();
    final var endExclusive = bookingDto2.getDepartureDate();

    final var availabilities = bookingService.getAvailabilities(startInclusive, endExclusive);

    final var expectedAvailabilities = Utils.getDatesBetween(
        bookingDto1.getDepartureDate(),
        bookingDto2.getArrivalDate());
    assertThat(availabilities).containsExactlyElementsOf(expectedAvailabilities);
  }
}