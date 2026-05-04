package sk.sporixx.dto;

import lombok.Builder;
import lombok.Data;

/** DTO pre nastavenia používateľa
 */
@Data
@Builder
public class UserSettings {
    private String languageCode; // "sk_SK", "en_US"
    private String currencyCode; // "EUR", "USD", "GBP", "CZK", "PLN"
    private String currencySymbol; // "€", "$"...
    private String decimalSeparator; // z regions.decimal_separator
    private String thousandsSeparator; // z regions.thousands_separator
    private String dateFormat; // "dd.MM.yyyy" alebo "MM/dd/yyyy"
    private String timeFormat; // "HH:mm" alebo "hh:mm a"
}
