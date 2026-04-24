package sk.sporixx.service;

import lombok.Getter;

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
