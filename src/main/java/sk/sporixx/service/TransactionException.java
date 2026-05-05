package sk.sporixx.service;

import lombok.Getter;

/**
 * Výnimka pre chyby pri transakciách.
 */
@Getter
public class TransactionException extends RuntimeException {

    private final String messageKey;

    public TransactionException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }

    public TransactionException(String messageKey, Throwable cause) {
        super(messageKey, cause);
        this.messageKey = messageKey;
    }
}
