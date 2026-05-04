package sk.sporixx.service;

import lombok.Getter;

/**
 * Výnimka pre chyby pri recurring rules (vytvorenie, editácia).
 */
@Getter
public class RecurringRuleException extends RuntimeException {
    private final String messageKey;

    public RecurringRuleException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }

    public RecurringRuleException(String messageKey, Throwable cause) {
        super(messageKey, cause);
        this.messageKey = messageKey;
    }
}
