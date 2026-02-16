package io.fleetcoreplatform.Exceptions;

public class KinesisCannotCreateChannelException extends RuntimeException {
    public KinesisCannotCreateChannelException(String message) {
        super(message);
    }
}
