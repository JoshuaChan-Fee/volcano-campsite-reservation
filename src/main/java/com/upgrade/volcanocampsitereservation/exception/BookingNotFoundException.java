package com.upgrade.volcanocampsitereservation.exception;

public class BookingNotFoundException extends RuntimeException {
  public BookingNotFoundException(long id) {
    super("Could not find booking with id " + id);
  }
}
