package com.softmotions.qxmaven;


public class PathResolutionException extends Exception {
    public PathResolutionException(String message) {
        super(message);
    }

    public PathResolutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public PathResolutionException(Throwable cause) {
        super(cause);
    }
}
