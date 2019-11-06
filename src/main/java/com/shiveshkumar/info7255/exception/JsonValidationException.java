package com.shiveshkumar.info7255.exception;

public class JsonValidationException extends RuntimeException {
   
	private static final long serialVersionUID = -3231859437946692260L;

	public JsonValidationException(String message) {
        super(message);
    }
}
