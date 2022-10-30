package com.upgrade.volcanocampsitereservation.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = BookingValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface BookingGuideLine {
  String message() default "The booking does not respect the guidelines";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
