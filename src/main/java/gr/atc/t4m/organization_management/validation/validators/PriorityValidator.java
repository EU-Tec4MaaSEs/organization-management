package gr.atc.t4m.organization_management.validation.validators;

import gr.atc.t4m.organization_management.model.MessagePriority;
import gr.atc.t4m.organization_management.validation.ValidPriority;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.EnumUtils;

public class PriorityValidator implements ConstraintValidator<ValidPriority, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null)
            return true;

       return EnumUtils.isValidEnumIgnoreCase(MessagePriority.class, value);
    }
}
