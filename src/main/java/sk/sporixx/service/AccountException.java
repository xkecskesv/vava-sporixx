package sk.sporixx.service;

import lombok.Getter;

/**
 * Výnimka pre chyby pri účtoch (vytváranie, editovanie).
 * UI vrstva ju chytí a zobrazí lokalizovanú hlášku podľa messageKey.
 */
@Getter
public class AccountException extends RuntimeException {

    private final String messageKey;

    public AccountException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }

    public AccountException(String messageKey, Throwable cause) {
        super(messageKey, cause);
        this.messageKey = messageKey;
    }
}