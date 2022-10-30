package com.upgrade.volcanocampsitereservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.upgrade.volcanocampsitereservation.domain.Booking;
import com.upgrade.volcanocampsitereservation.dto.BookingDateDto;
import com.upgrade.volcanocampsitereservation.dto.BookingDto;
import com.upgrade.volcanocampsitereservation.mock.MockUtils;
import com.upgrade.volcanocampsitereservation.repository.BookingDateRepository;
import com.upgrade.volcanocampsitereservation.repository.BookingRepository;
import com.upgrade.volcanocampsitereservation.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.upgrade.volcanocampsitereservation.controller.BookingController.BASE_AVAILABLE_PATH;
import static com.upgrade.volcanocampsitereservation.controller.BookingController.BASE_BOOKING_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test the {@link BookingController}.
 * <p>Mock the Repository classes.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class BookingControllerTest {
  private final ObjectMapper objectMapper = new ObjectMapper();

  @MockBean
  private BookingRepository bookingRepository;

  @MockBean
  private BookingDateRepository bookingDateRepository;

  @Autowired
  private MockMvc mockMvc;

  @SpyBean
  private BookingService bookingService;

  private static Stream<Arguments> addBooking_invalid_source() {
    return Stream.of(
        Arguments.of(MockUtils.createBookingTooEarly(), "The campsite can be reserved minimum 1 day(s) ahead of arrival."),
        Arguments.of(MockUtils.createTooLongBooking(), "The campsite can be reserved for maximum 3 days."),
        Arguments.of(MockUtils.createTooShortBooking(), "The campsite can be reserved for minimum 1 day."),
        Arguments.of(MockUtils.createBookingTooLate(), "The campsite can be reserved up to 31 day(s) in advance."),
        Arguments.of(MockUtils.createBookingWithDepartureBeforeArrival(), "Arrival date should be before departure date.")
    );
  }

  private static Stream<Arguments> addBooking_invalidDate_source() {
    return Stream.of(
        Arguments.of("{\"departureDate\":\"invalid\"}", "Text 'invalid' could not be parsed"),
        Arguments.of("{\"departureDate\":\"2023-02-31\"}", "Invalid date"),
        Arguments.of("invalid json", "Unrecognized token")
    );
  }

  @BeforeEach
  void setUp() {
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
  }

  @Test
  void getBookingList_success() throws Exception {
    final var bookingDtoList = List.of(MockUtils.createBookingDtoWithId());
    final var bookingList = bookingDtoList.stream().map(Booking::createFrom).collect(Collectors.toList());
    final var bookingListJson = objectMapper.writeValueAsString(bookingList);
    when(bookingRepository.findAll(any(Sort.class))).thenReturn(bookingDtoList);
    mockMvc.perform(get(BASE_BOOKING_PATH))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(bookingListJson));
  }

  @Test
  void getBookingDtoList_empty() throws Exception {
    final List<BookingDto> bookingDtoList = List.of();
    final var bookingDtoListJson = objectMapper.writeValueAsString(bookingDtoList);
    when(bookingRepository.findAll(any(Sort.class))).thenReturn(bookingDtoList);
    mockMvc.perform(get(BASE_BOOKING_PATH))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(bookingDtoListJson));
  }

  @Test
  void getBooking_success() throws Exception {
    final var bookingDto = MockUtils.createBookingDtoWithId();
    final var bookingDtoJson = objectMapper.writeValueAsString(Booking.createFrom(bookingDto));
    when(bookingRepository.findById(bookingDto.getId())).thenReturn(Optional.of(bookingDto));
    mockMvc.perform(get(BASE_BOOKING_PATH + "/" + bookingDto.getId()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(bookingDtoJson));
  }

  @Test
  void getBooking_unknown() throws Exception {
    final var bookingDto = MockUtils.createBookingDtoWithId();
    when(bookingRepository.findById(bookingDto.getId())).thenReturn(Optional.empty());
    mockMvc.perform(get(BASE_BOOKING_PATH + "/" + bookingDto.getId()))
        .andDo(print())
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().string(containsString("Could not find booking with id " + bookingDto.getId())));
  }

  @Test
  void addBooking_success() throws Exception {
    final var booking = MockUtils.createValidBooking();
    final var bookingJson = objectMapper.writeValueAsString(booking);
    final var expectedBookingDto = BookingDto.createFrom(booking);
    final var expectedBookingJson = objectMapper.writeValueAsString(booking);
    when(bookingRepository.save(expectedBookingDto)).then(returnsFirstArg());
    mockMvc.perform(post(BASE_BOOKING_PATH).contentType(MediaType.APPLICATION_JSON).content(bookingJson))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(expectedBookingJson));
  }

  @ParameterizedTest
  @MethodSource("addBooking_invalid_source")
  void addBooking_invalid(Booking booking, String message) throws Exception {
    final var bookingJson = objectMapper.writeValueAsString(booking);
    mockMvc.perform(post(BASE_BOOKING_PATH).contentType(MediaType.APPLICATION_JSON).content(bookingJson))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().string(containsString(message)));
  }

  @ParameterizedTest
  @MethodSource("addBooking_invalidDate_source")
  void addBooking_invalidDate(String json, String message) throws Exception {
    mockMvc.perform(post(BASE_BOOKING_PATH).contentType(MediaType.APPLICATION_JSON).content(json))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().string(containsString(message)));
  }

  @Test
  void addBooking_internalServerError() throws Exception {
    final var booking = MockUtils.createValidBooking();
    final var bookingJson = objectMapper.writeValueAsString(booking);
    when(bookingRepository.save(any())).thenThrow(new RuntimeException("exception message"));
    mockMvc.perform(post(BASE_BOOKING_PATH).contentType(MediaType.APPLICATION_JSON).content(bookingJson))
        .andDo(print())
        .andExpect(status().isInternalServerError())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().string(containsString("java.lang.RuntimeException: exception message")));
  }

  @Test
  void addBooking_notAvailable() throws Exception {
    final var booking = MockUtils.createValidBooking();
    final var bookingJson = objectMapper.writeValueAsString(booking);
    // A date within the booking dates is already booked
    final var alreadyBookedDate = booking.getArrivalDate();
    when(bookingDateRepository.findAllDatesBetween(any(), any()))
        .thenReturn(Stream.of(BookingDateDto.builder().date(alreadyBookedDate).build()));
    mockMvc.perform(post(BASE_BOOKING_PATH).contentType(MediaType.APPLICATION_JSON).content(bookingJson))
        .andDo(print())
        .andExpect(status().isConflict())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().string(containsString(
            MessageFormat.format("Dates [{0}] are not available", alreadyBookedDate))));
  }

  @Test
  void updateBooking() throws Exception {
    final var oldBooking = MockUtils.createBooking(LocalDate.now().plusDays(2), 2);
    final var newBooking = MockUtils.createBooking(LocalDate.now().plusDays(2), 3);
    final var bookingJson = objectMapper.writeValueAsString(newBooking);
    final var expectedBookingDto = BookingDto.createFrom(newBooking);
    when(bookingRepository.findById(any())).thenReturn(Optional.of(BookingDto.createFrom(oldBooking)));
    when(bookingRepository.save(expectedBookingDto)).then(returnsFirstArg());
    mockMvc.perform(put(BASE_BOOKING_PATH + "/1").contentType(MediaType.APPLICATION_JSON).content(bookingJson))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json(bookingJson));
  }

  @Test
  void deleteBooking_success() throws Exception {
    mockMvc.perform(delete(BASE_BOOKING_PATH + "/1"))
        .andDo(print())
        .andExpect(status().isOk());
  }

  @Test
  void deleteBooking_failure() throws Exception {
    doThrow(EmptyResultDataAccessException.class).when(bookingRepository).deleteById(any());
    mockMvc.perform(delete(BASE_BOOKING_PATH + "/1"))
        .andDo(print())
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().string(containsString("Could not find booking with id 1")));
  }

  @Test
  void getBookingAvailabilities_noBooking() throws Exception {
    mockMvc.perform(get(BASE_AVAILABLE_PATH)
        .queryParam("startDate", "2022-01-28")
        .queryParam("endDate", "2022-02-03"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json("[2022-01-28, 2022-01-29, 2022-01-30, 2022-01-31, 2022-02-01, 2022-02-02]"));
  }

  @Test
  void getBookingAvailabilities_1Booking() throws Exception {
    final var bookingDates = MockUtils.bookingDatesDto("2022-01-29", 2);
    when(bookingDateRepository.quickFindAllDatesBetween(any(), any())).thenReturn(bookingDates);
    mockMvc.perform(get(BASE_AVAILABLE_PATH)
        .queryParam("startDate", "2022-01-28")
        .queryParam("endDate", "2022-02-03"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json("[2022-01-28, 2022-01-31, 2022-02-01, 2022-02-02]"));
  }

  @Test
  void getBookingAvailabilities_2Bookings() throws Exception {
    final var bookingDatesDto = Stream.concat(
        MockUtils.bookingDatesDto("2022-01-29", 2),
        MockUtils.bookingDatesDto("2022-02-01", 1));
    when(bookingDateRepository.quickFindAllDatesBetween(any(), any())).thenReturn(bookingDatesDto);
    mockMvc.perform(get(BASE_AVAILABLE_PATH)
        .queryParam("startDate", "2022-01-28")
        .queryParam("endDate", "2022-02-03"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json("[2022-01-28, 2022-01-31, 2022-02-02]"));
  }

  @Test
  void getBookingAvailabilities_1Booking_overlapStartDate() throws Exception {
    final var bookingDatesDto = MockUtils.bookingDatesDto("2022-01-27", 3);
    when(bookingDateRepository.quickFindAllDatesBetween(any(), any())).thenReturn(bookingDatesDto);
    mockMvc.perform(get(BASE_AVAILABLE_PATH)
        .queryParam("startDate", "2022-01-28")
        .queryParam("endDate", "2022-02-03"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json("[2022-01-30, 2022-01-31, 2022-02-01, 2022-02-02]"));
  }

  @Test
  void getBookingAvailabilities_1Booking_overlapEndDate() throws Exception {
    final var bookingDatesDto = MockUtils.bookingDatesDto("2022-02-02", 3);
    when(bookingDateRepository.quickFindAllDatesBetween(any(), any())).thenReturn(bookingDatesDto);
    mockMvc.perform(get(BASE_AVAILABLE_PATH)
        .queryParam("startDate", "2022-01-28")
        .queryParam("endDate", "2022-02-03"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json("[2022-01-28, 2022-01-29, 2022-01-30, 2022-01-31, 2022-02-01]"));
  }

  @Test
  void getBookingAvailabilities_no_param() throws Exception {
    final var argumentCaptor = ArgumentCaptor.forClass(LocalDate.class);

    mockMvc.perform(get(BASE_AVAILABLE_PATH))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));

    verify(bookingService).getAvailabilities(argumentCaptor.capture(), argumentCaptor.capture());
    assertThat(argumentCaptor.getAllValues()).containsExactly(LocalDate.now(), LocalDate.now().plusMonths(1));
  }

  @Test
  void getBookingAvailabilities_start_param() throws Exception {
    final var argumentCaptor = ArgumentCaptor.forClass(LocalDate.class);
    final var start = "2022-01-28";
    final var startDate = LocalDate.parse(start);

    mockMvc.perform(get(BASE_AVAILABLE_PATH)
        .queryParam("startDate", start))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));

    verify(bookingService).getAvailabilities(argumentCaptor.capture(), argumentCaptor.capture());
    assertThat(argumentCaptor.getAllValues()).containsExactly(startDate, startDate.plusMonths(1));
  }

  @Test
  void getBookingAvailabilities_end_param() throws Exception {
    final var argumentCaptor = ArgumentCaptor.forClass(LocalDate.class);
    final var endDate = LocalDate.now().plusDays(5);

    mockMvc.perform(get(BASE_AVAILABLE_PATH)
        .queryParam("endDate", endDate.toString()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));

    verify(bookingService).getAvailabilities(argumentCaptor.capture(), argumentCaptor.capture());
    assertThat(argumentCaptor.getAllValues()).containsExactly(LocalDate.now(), endDate);
  }

  @Test
  void getBookingAvailabilities_start_after_end() throws Exception {
    mockMvc.perform(get(BASE_AVAILABLE_PATH)
        .queryParam("startDate", "2022-01-02")
        .queryParam("endDate", "2022-01-01"))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().string(containsString("Start date 2022-01-02 is after end date 2022-01-01")));
  }

}