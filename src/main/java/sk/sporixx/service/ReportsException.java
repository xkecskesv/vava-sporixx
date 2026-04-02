package sk.sporixx.service;

import lombok.Getter;

@Getter
public class ReportsException extends RuntimeException {

    private final String messageKey;

    public ReportsException(String messageKey, Throwable cause) {
        super(messageKey, cause);
        this.messageKey = messageKey;
    }
}
