package com.shiveshkumar.info7255.exception;

public class ApiResouceAlreadyExistException extends RuntimeException {
 
	private static final long serialVersionUID = -4086103423150597158L;

	public ApiResouceAlreadyExistException(String message) {
        super(message);
    }
}
