package sk.sporixx.service;

import lombok.Getter;

/**
 * Výnimka pre chyby na Overview obrazovke.
 * UI vrstva ju chytí a zobrazí lokalizovanú hlášku podľa messageKey.
 */
@Getter
public class OverviewException extends RuntimeException {

    private final String messageKey;

    public OverviewException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }

    public OverviewException(String messageKey, Throwable cause) {
        super(messageKey, cause);
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
