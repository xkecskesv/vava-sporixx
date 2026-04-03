package sk.sporixx.service;

import lombok.Getter;

/**
 * Exception for profile update and password change validation/business errors.
 */
@Getter
public class ProfileException extends RuntimeException {

    private final String messageKey;

    public ProfileException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }

    public ProfileException(String messageKey, Throwable cause) {
        super(messageKey, cause);
        this.messageKey = messageKey;
    }
}

