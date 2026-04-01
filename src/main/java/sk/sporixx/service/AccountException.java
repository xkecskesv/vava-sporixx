package sk.sporixx.service;

public class AccountException extends RuntimeException {

    private final String messageKey;

    public AccountException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}