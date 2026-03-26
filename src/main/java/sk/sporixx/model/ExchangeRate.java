package sk.sporixx.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Datový model pre výmenný kurz.
 * Príklad: 1 EUR = 1.08 USD -> base="EUR", target="USD", rate=1.08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRate {

    private int id;
    private String baseCurrencyCode;
    private String targetCurrencyCode;
    private double rate;
    private LocalDateTime capturedAt;
}