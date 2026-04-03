package sk.sporixx.service;

import lombok.Getter;

@Getter
public class ExportException extends RuntimeException {

    private final String messageKey;

    public ExportException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }

    public ExportException(String messageKey, Throwable cause) {
        super(messageKey, cause);
        this.messageKey = messageKey;
    }
}
