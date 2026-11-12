package com.moodtune.data;

public final class JamendoException extends RuntimeException {
    public JamendoException(String message) {
        super(message);
    }

    public JamendoException(String message, Throwable cause) {
        super(message, cause);
    }
}
