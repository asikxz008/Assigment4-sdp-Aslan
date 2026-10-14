package com.aitu.sdp.media;

/** One checked failure type for clients of the simplified conversion API. */
public final class ConversionException extends Exception {
    private static final long serialVersionUID = 1L;

    public ConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
