package sk.sporixx.service;

import lombok.Getter;

/**
 * Výnimka pre chyby pri autentifikácii (login, register, zmena hesla).
 * UI vrstva ju chytí a zobrazí lokalizovanú hlášku podľa messageKey.
 */
@Getter
public class AuthException extends RuntimeException {

    private final String messageKey;

    public AuthException(String messageKey) {
        super("Auth error with key: " + messageKey);
        this.messageKey = messageKey;
    }

    public AuthException(String messageKey, Throwable cause) {
        super("Auth error with key: " + messageKey, cause);
        this.messageKey = messageKey;
    }
}
