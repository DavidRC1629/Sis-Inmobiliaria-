package com.sisarovi.inmobiliario.exception;

public class ReniecServiceUnavailableException extends RuntimeException {
    private final String detail;

    public ReniecServiceUnavailableException(String message, String detail) {
        super(message);
        this.detail = detail;
    }

    public String getDetail() {
        return detail;
    }
}