package com.upgrade.volcanocampsitereservation.service;

import com.upgrade.volcanocampsitereservation.mock.MockUtils;
import com.upgrade.volcanocampsitereservation.repository.BookingDateRepository;
import com.upgrade.volcanocampsitereservation.dto.BookingDto;
import com.upgrade.volcanocampsitereservation.repository.BookingRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.dao.TransientDataAccessException;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;

/**
 * Concurrency tests for the class {@link BookingService}.
 * <p>This test class uses the h2 in-memory database.</p>
 * <p>A delay is artificially added inside the method {@link BookingService#add(BookingDto)} to make sure a second
 * call (from another thread) occurs during the execution of the first call. The expected result is the second </p>
 */
@SpringBootTest
@Slf4j
class BookingServiceConcurrencyTest {

  private static final int DELAY = 100;

  @SpyBean
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

  /**
   * Test the creation of 2 entities (without overlapping date) in parallel. Both should be successful.
   */
  @Test
  void addWithConcurrency_no_conflict() throws Exception {
    final var bookingDto1 = MockUtils.createBookingDto();
    final var bookingDto2 = MockUtils.createAnotherBookingDto();
    insertDelayInsideLockingSection();
    final var executor = Executors.newFixedThreadPool(2);

    // Creation of the first entity (should be successful)
    final var future1 = executor.submit(() -> bookingService.add(bookingDto1));

    // Creation of the second entity (should be successful)
    final var future2 = executor.submit(() -> bookingService.add(bookingDto2));

    executor.shutdown();
    assertThat(executor.awaitTermination(DELAY * 4, TimeUnit.MILLISECONDS)).isTrue();
    assertThat(future1.get()).isEqualTo(bookingDto1);
    assertThat(future2.get()).isEqualTo(bookingDto2);
    assertThat(bookingRepository.findAll()).containsExactlyInAnyOrder(bookingDto1, bookingDto2);
    var expectedBookingDates = bookingDto1.bookingDates();
    expectedBookingDates.addAll(bookingDto2.bookingDates());
    assertThat(bookingDateRepository.findAll()).containsExactlyElementsOf(expectedBookingDates);
  }

  /**
   * Test the creation of 2 entities (with overlapping date) in parallel. The second one should fail.
   */
  @Test
  void addWithConcurrency_conflict() throws Exception {
    final var bookingDto1 = MockUtils.createBookingDto();
    final var bookingDto2 = createOverlappedBookingDto(bookingDto1);
    insertDelayInsideLockingSection();
    final var executor = Executors.newFixedThreadPool(2);

    // Creation of the first entity (should be successful)
    final var future1 = executor.submit(() -> bookingService.add(bookingDto1));

    // Wait a bit to make sure the first entity wins.
    TimeUnit.MILLISECONDS.sleep(DELAY / 2);

    // Creation of the second conflicting entity (should fail)
    final var future2 = executor.submit(() -> bookingService.add(bookingDto2));

    executor.shutdown();
    assertThat(executor.awaitTermination(DELAY * 4, TimeUnit.MILLISECONDS)).isTrue();
    assertThat(future1.get()).isEqualTo(bookingDto1);
    // NonTransientDataAccessException means a retry of the failed operation would fail.
    assertThatThrownBy(future2::get).hasCauseInstanceOf(NonTransientDataAccessException.class);
    assertThat(bookingRepository.findAll()).containsExactly(bookingDto1);
    assertThat(bookingDateRepository.findAll()).containsExactlyElementsOf(bookingDto1.bookingDates());
  }

  /**
   * Test the creation of multiple identical entities in parallel. Only 1 should be successful.
   */
  @ParameterizedTest
  @ValueSource(ints = {3, 5, 10, 50})
  void addWithConcurrency_multiple_conflicts(int num) throws Exception {
    final var bookingDto = MockUtils.createBookingDto();
    final var executor = Executors.newFixedThreadPool(num);

    for (int i = 0; i < num; i++) {
      executor.execute(() -> bookingService.add(bookingDto));
    }

    executor.shutdown();
    assertThat(executor.awaitTermination(DELAY * 4, TimeUnit.MILLISECONDS)).isTrue();
    assertThat(bookingRepository.findAll()).hasSize(1);
    var expectedBookingDates = bookingDto.bookingDates();
    assertThat(bookingDateRepository.findAll()).containsExactlyElementsOf(expectedBookingDates);
  }

  /**
   * Test the concurrent update of 1 entity. The second one should fail.
   */
  @Test
  void updateWithConcurrency() throws Exception {
    // Creation of 1 entity (should be successful)
    final BookingDto addedBookingDto = bookingService.add(MockUtils.createBookingDto());
    assertThat(bookingRepository.findAll()).containsExactly(addedBookingDto);
    assertThat(bookingDateRepository.findAll()).containsExactlyElementsOf(addedBookingDto.bookingDates());

    final var bookingDto1 = createOverlappedBookingDto(addedBookingDto);
    final var bookingDto2 = createOverlappedBookingDto(bookingDto1);
    insertDelayInsideLockingSection();
    final var executor = Executors.newFixedThreadPool(2);

    // Update of the first entity (should be successful)
    var future1 = executor.submit(() -> bookingService.update(addedBookingDto, bookingDto1));
    TimeUnit.MILLISECONDS.sleep(DELAY / 2);

    // Second update of the same entity (should fail)
    var future2 = executor.submit(() -> bookingService.update(addedBookingDto, bookingDto2));
    executor.shutdown();
    assertThat(executor.awaitTermination(DELAY * 4, TimeUnit.MILLISECONDS)).isTrue();

    // The first update will increase the version of the updated entity
    bookingDto1.setVersion(bookingDto1.getVersion() + 1);
    assertThat(future1.get()).isEqualTo(bookingDto1);
    // TransientDataAccessException means a retry of the failed operation would succeed.
    assertThatThrownBy(future2::get).hasCauseInstanceOf(TransientDataAccessException.class);
    assertThat(bookingRepository.findAll()).containsExactly(bookingDto1);
    assertThat(bookingDateRepository.findAll()).containsExactlyElementsOf(bookingDto1.bookingDates());
  }

  private void insertDelayInsideLockingSection() {
    doAnswer(invocation -> {
      try {
        log.info("Inserting delay of {} ms inside the locking section", DELAY);
        TimeUnit.MILLISECONDS.sleep(DELAY);
      } catch (InterruptedException ignore) {
        Thread.currentThread().interrupt();
      }
      return null;
    }).when(bookingService).testArtificialDelay();
  }

  private BookingDto createOverlappedBookingDto(BookingDto bookingDto) {
    return BookingDto.builder()
        .email("test@email.com")
        .fullName("TestTesting")
        .arrivalDate(bookingDto.getArrivalDate().minusDays(1))
        .departureDate(bookingDto.getDepartureDate())
        .build();
  }
}