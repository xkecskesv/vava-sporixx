package sk.sporixx.service;

public interface CurrencyService {
    void refreshRates();
    double convert(double amount, String fromCurrency, String toCurrency);
    String format(double amount, String targetCurrency);
    String getUserCurrency();
}