package com.upgrade.volcanocampsitereservation.validation;

import com.upgrade.volcanocampsitereservation.domain.Booking;
import com.upgrade.volcanocampsitereservation.config.ApplicationConfiguration;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.util.Objects;

import static java.time.temporal.ChronoUnit.DAYS;

@Component
public class BookingValidator implements ConstraintValidator<BookingGuideLine, Booking> {
  private final ApplicationConfiguration rsvpConfig;

  public BookingValidator(ApplicationConfiguration rsvpConfig) {
    this.rsvpConfig = rsvpConfig;
  }

  @Override
  public void initialize(BookingGuideLine constraintAnnotation) {
    ConstraintValidator.super.initialize(constraintAnnotation);
  }

  @Override
  public boolean isValid(Booking booking, ConstraintValidatorContext context) {
    final var arrivalDate = booking.getArrivalDate();
    final var departureDate = booking.getDepartureDate();
    final var daysAheadOfArrival = DAYS.between(LocalDate.now(), arrivalDate);
    var valid = true;

    if (Objects.isNull(arrivalDate) || Objects.isNull(departureDate)) {
      // null values are valid
      return true;
    }
    context.disableDefaultConstraintViolation();

    // Arrival date should be before departure date
    if (departureDate.isBefore(arrivalDate)) {
      valid = invalidErrorMessage(context, "Arrival date should be before departure date");
    }

    // The campsite can be reserved for min 1 days.
    final var stayInDays = DAYS.between(arrivalDate, departureDate);
    if (stayInDays < 1) {
      valid = invalidErrorMessage(context, "The campsite can be reserved for minimum 1 day");
    }

    // The campsite can be reserved for max 3 days.
    final var maxStayInDays = rsvpConfig.getMaxReservedDays();
    if (stayInDays > maxStayInDays) {
      valid = invalidErrorMessage(context, "The campsite can be reserved for maximum " + maxStayInDays + " days");
    }

    // The campsite can be reserved minimum 1 day(s) ahead of arrival
    final var minDaysAheadOfArrival = rsvpConfig.getMinDaysAheadOfArrival();
    if (daysAheadOfArrival < minDaysAheadOfArrival) {
      valid = invalidErrorMessage(context, "The campsite can be reserved minimum " + minDaysAheadOfArrival + " day(s) ahead of arrival");
    }

    // The campsite can be reserved up to 1 month in advance.
    final var maxDaysAheadOfArrival = rsvpConfig.getReservationMaxDaysInAdvance();
    if (daysAheadOfArrival > maxDaysAheadOfArrival) {
      valid = invalidErrorMessage(context, "The campsite can be reserved up to " + maxDaysAheadOfArrival + " day(s) in advance");
    }
    return valid;
  }

  private boolean invalidErrorMessage(ConstraintValidatorContext context, String message) {
    context
        .buildConstraintViolationWithTemplate(message)
        .addConstraintViolation();
    return false;
  }

}
