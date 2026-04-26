package sk.sporixx.service;

import lombok.Getter;

@Getter
public class ImportException extends RuntimeException {

  private final String messageKey;

  public ImportException(String messageKey) {
    super(messageKey);
    this.messageKey = messageKey;
  }

  public ImportException(String messageKey, Throwable cause) {
    super(messageKey, cause);
    this.messageKey = messageKey;
  }

}