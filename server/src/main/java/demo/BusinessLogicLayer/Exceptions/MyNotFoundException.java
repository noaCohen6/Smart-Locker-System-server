package demo.BusinessLogicLayer.Exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class MyNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1487178726665525952L;

    public MyNotFoundException() {
        super();
    }

    public MyNotFoundException(String message) {
        super(message);
    }

    public MyNotFoundException(Exception cause) {
        super (cause);
    }

    public MyNotFoundException(String message, Exception cause) {
        super(message, cause);
    }
}


