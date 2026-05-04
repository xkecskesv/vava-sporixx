package sk.sporixx.service;

import lombok.Getter;

/**
 * Výnimka pre chyby pri family managemente.
 */
@Getter
public class FamilyException extends RuntimeException {

    private final String messageKey;

    public FamilyException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }

    public FamilyException(String messageKey, Throwable cause) {
        super(messageKey, cause);
        this.messageKey = messageKey;
    }
}
