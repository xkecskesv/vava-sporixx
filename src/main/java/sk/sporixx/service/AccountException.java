package sk.sporixx.service;

import lombok.Getter;

@Getter
public class AccountException extends RuntimeException {

    private final String messageKey;

    public AccountException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }

}