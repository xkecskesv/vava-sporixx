package sk.sporixx.service;

import lombok.Getter;

/**
 * Výnimka pre chyby pri autentifikácii (login, register, zmena hesla).
 */
@Getter
public class AuthException extends RuntimeException {

    private final String messageKey;

    public AuthException(String messageKey, String message) {
        super(message);
        this.messageKey = messageKey;
    }

    public AuthException(String messageKey, String message, Throwable cause) {
        super(message, cause);
        this.messageKey = messageKey;
    }
}
