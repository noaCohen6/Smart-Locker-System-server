package demo.BusinessLogicLayer.Exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.FORBIDDEN)
public class MyForbiddenException extends RuntimeException {
    private static final long serialVersionUID = 1407040926062801123L;

    public MyForbiddenException() {
    }

    public MyForbiddenException(String message) {
        super(message);
    }

    public MyForbiddenException(Throwable cause) {
        super(cause);
    }

    public MyForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
