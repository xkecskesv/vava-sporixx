package sk.sporixx.service;

import lombok.Getter;

/**
 * Runtime exception used for profile validation and business-rule failures.
 *
 * <p>The message value stores a localization key consumed by the UI layer.</p>
 *
 * @author Viktória Kecskés
 */
@Getter
public class ProfileException extends RuntimeException {

    private final String messageKey;

    /**
     * Creates a new profile exception with a localization message key.
     *
     * @param messageKey localization key for UI error rendering
     */
    public ProfileException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }

    /**
     * Creates a new profile exception with a localization key and root cause.
     *
     * @param messageKey localization key for UI error rendering
     * @param cause original failure cause
     */
    public ProfileException(String messageKey, Throwable cause) {
        super(messageKey, cause);
        this.messageKey = messageKey;
    }
}

