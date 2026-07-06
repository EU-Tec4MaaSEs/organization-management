package gr.atc.t4m.organization_management.exception;

import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;


@ResponseStatus(HttpStatus.CONFLICT)
public class DomainInUseException extends RuntimeException {
    public DomainInUseException(String message) {
        super(message);
    }
}

