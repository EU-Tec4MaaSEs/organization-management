package gr.atc.t4m.organization_management.exception;

import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidOrganizationRoleException extends RuntimeException {
    public InvalidOrganizationRoleException(String message) {
        super(message);
    }
}
