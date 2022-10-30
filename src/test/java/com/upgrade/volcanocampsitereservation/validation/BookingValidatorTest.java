package com.upgrade.volcanocampsitereservation.validation;

import com.upgrade.volcanocampsitereservation.domain.Booking;
import com.upgrade.volcanocampsitereservation.mock.MockUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.validation.ConstraintValidatorContext;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
class BookingValidatorTest {

  @Autowired
  private BookingValidator bookingValidator;

  @Mock
  private ConstraintValidatorContext context;

  private static Stream<Arguments> invalidBookingSource() {
    return Stream.of(
        Arguments.of(MockUtils.createBookingTooEarly()),
        Arguments.of(MockUtils.createBookingTooLate()),
        Arguments.of(MockUtils.createTooLongBooking()),
        Arguments.of(MockUtils.createTooShortBooking())
    );
  }

  @ParameterizedTest
  @MethodSource("invalidBookingSource")
  void testInvalid(Booking booking) {
    when(context.buildConstraintViolationWithTemplate(any()))
        .thenReturn(mock(ConstraintValidatorContext.ConstraintViolationBuilder.class));

    assertThat(bookingValidator.isValid(booking, context)).isFalse();
  }

  @Test
  void testValid() {
    assertThat(bookingValidator.isValid(MockUtils.createValidBooking(), context)).isTrue();
  }
}