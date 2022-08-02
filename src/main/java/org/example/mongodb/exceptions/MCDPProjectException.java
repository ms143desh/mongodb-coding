package org.example.mongodb.exceptions;

public class MCDPProjectException extends RuntimeException { 

	private static final long serialVersionUID = 1L;

	public MCDPProjectException(String errorMessage) {
        super(errorMessage);
    }
    
    public MCDPProjectException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
    
}