package sk.sporixx.service;

import lombok.Getter;

@Getter
public class CategoryException extends RuntimeException {

    private final String messageKey;

    public CategoryException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }

    public CategoryException(String messageKey, Throwable cause) {
        super(messageKey, cause);
        this.messageKey = messageKey;
    }
}