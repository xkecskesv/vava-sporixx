package sk.sporixx.service;

import lombok.Getter;

/**
 * Všeobecná výnimka pre service vrstvu.
 * Používa sa keď operácia v service vrstve zlyhá (napr. DB chyba).
 */
@Getter
public class ServiceException extends RuntimeException {

    private final String messageKey;

    public ServiceException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }

    public ServiceException(String messageKey, Throwable cause) {
        super(messageKey, cause);
        this.messageKey = messageKey;
    }
}