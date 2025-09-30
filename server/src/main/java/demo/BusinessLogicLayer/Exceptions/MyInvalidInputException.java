package demo.BusinessLogicLayer.Exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST)

public class MyInvalidInputException extends RuntimeException {
    private static final long serialVersionUID = 3408236811942129993L;

    public MyInvalidInputException() {
    }
    
    public MyInvalidInputException(String message) {
        super(message);
    }

    public MyInvalidInputException(Throwable cause) {
        super(cause);
    }

    public MyInvalidInputException(String message, Throwable cause) {
        super(message, cause);
    }
}

