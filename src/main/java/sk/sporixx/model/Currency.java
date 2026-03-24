package sk.sporixx.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Datový model pre menu.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Currency {

    private int id;
    private String code;    // "EUR", "USD", "PLN"
    private String name;    // "Euro", "US Dollar"
    private String symbol;  // "€", "$"
}
