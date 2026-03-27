package sk.sporixx.service;

import lombok.Getter;

/**
 * Výnimka pre chyby pri autentifikácii (login, register).
 * UI vrstva ju chytí a zobrazí lokalizovanú hlášku podľa messageKey.
 */
@Getter
public class AuthException extends RuntimeException {

    private final String messageKey;

    public AuthException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }

    public AuthException(String messageKey, Throwable cause) {
        super(messageKey, cause);
        this.messageKey = messageKey;
    }
}
