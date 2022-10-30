package com.upgrade.volcanocampsitereservation.exception;

import com.google.common.base.Throwables;
import com.upgrade.volcanocampsitereservation.validation.BookingValidator;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import org.springframework.beans.BeansException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.stream.Collectors;

@ControllerAdvice
public class BookingControllerAdvice {

  private ErrorResponse errorHandler(HttpStatus status, Throwable throwable) {
    return ErrorResponse.builder()
        .status(status)
        .message(Throwables.getRootCause(throwable).getMessage())
        .build();
  }

  @ResponseBody
  @ExceptionHandler(BadRequestException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  ErrorResponse badRequestHandler(BadRequestException e) {
    return errorHandler(HttpStatus.BAD_REQUEST, e);
  }

  /**
   * Error when parsing LocalDate as a property
   */
  @ResponseBody
  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  ErrorResponse dateTimeParsingHandler(HttpMessageNotReadableException e) {
    return errorHandler(HttpStatus.BAD_REQUEST, e);
  }

  /**
   * Error when parsing LocalDate as a request parameter
   */
  @ResponseBody
  @ExceptionHandler(BeansException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  ErrorResponse beansExceptionHandler(BeansException e) {
    return errorHandler(HttpStatus.BAD_REQUEST, e);
  }

  /**
   * Reformat the messages of the constraint violation (See {@link BookingValidator}.
   */
  @ResponseBody
  @ExceptionHandler(BindException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  ErrorResponse constraintViolationHandler(BindException e) {
    final var message = e.getBindingResult().getAllErrors()
        .stream()
        .map(DefaultMessageSourceResolvable::getDefaultMessage)
        .collect(Collectors.joining(". "))
        .concat(".");
    return ErrorResponse.builder()
        .status(HttpStatus.BAD_REQUEST)
        .message(message)
        .build();
  }

  @ResponseBody
  @ExceptionHandler(BookingNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  ErrorResponse notFoundHandler(BookingNotFoundException e) {
    return errorHandler(HttpStatus.NOT_FOUND, e);
  }

  @ResponseBody
  @ExceptionHandler(BookingConflictException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  ErrorResponse badRequestHandler(BookingConflictException e) {
    return errorHandler(HttpStatus.CONFLICT, e);
  }

  @ResponseBody
  @ExceptionHandler(RuntimeException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  ErrorResponse internalServerErrorHandler(RuntimeException e) {
    return ErrorResponse.builder()
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .message(Throwables.getRootCause(e).toString())
        .build();
  }

  @Value
  @Builder
  @Schema(title = "Error response", description = "Detailed information about an error")
  private static class ErrorResponse {
    @Schema(description = "HTTP status")
    HttpStatus status;
    @Schema(description = "Description of the error")
    String message;
  }
}
