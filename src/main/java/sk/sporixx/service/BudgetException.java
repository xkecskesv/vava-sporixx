package sk.sporixx.service;

import lombok.Getter;

@Getter
public class BudgetException extends RuntimeException {
    private final String messageKey;

    public BudgetException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }

    public BudgetException(String messageKey, Throwable cause) {
        super(messageKey, cause);
        this.messageKey = messageKey;
    }
}
