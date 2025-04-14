package gr.atc.t4m.organization_management.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

import gr.atc.t4m.organization_management.validation.validators.NotificationStatusValidator;

@Documented
@Constraint(validatedBy = NotificationStatusValidator.class)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidNotificationStatus {
    String message() default "Invalid assignment origin. Only 'Read' or 'Unread' are allowed.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
