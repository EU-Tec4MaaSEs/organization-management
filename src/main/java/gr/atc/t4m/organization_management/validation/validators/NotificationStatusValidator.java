package gr.atc.t4m.organization_management.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.EnumUtils;

import gr.atc.t4m.organization_management.model.NotificationStatus;
import gr.atc.t4m.organization_management.validation.ValidNotificationStatus;

public class NotificationStatusValidator implements ConstraintValidator<ValidNotificationStatus, String> {

    @Override
    public boolean isValid(String status, ConstraintValidatorContext context) {
        if (status == null)
            return false;

        return EnumUtils.isValidEnumIgnoreCase(NotificationStatus.class, status);
    }
}
