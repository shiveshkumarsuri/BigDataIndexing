package com.shiveshkumar.info7255.exception;

public class ApiResouceNotFoundException extends RuntimeException {
  
	private static final long serialVersionUID = -8034152993785816108L;

	public ApiResouceNotFoundException(String message) {
        super(message);
    }
}
