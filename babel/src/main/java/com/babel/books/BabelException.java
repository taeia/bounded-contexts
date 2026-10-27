package com.babel.books;

/**
 * Babel's own exception, thrown when a request is malformed. A real client would
 * also throw it when the network is down.
 */
public class BabelException extends RuntimeException {

    public BabelException(String message) {
        super(message);
    }
}
