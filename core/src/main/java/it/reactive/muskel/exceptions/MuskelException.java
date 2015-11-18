package it.reactive.muskel.exceptions;

public class MuskelException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MuskelException(String message) {
	super(message);
    }

    public MuskelException(Throwable t) {
	super(t);
    }

    public MuskelException(String message, Throwable t) {
	super(message, t);
    }

}
