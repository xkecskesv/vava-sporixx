package sk.sporixx.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * Kritériá pre vyhľadávanie a filtrovanie transakcií.
 * Používa sa v TransactionService.searchTransactions().
 * Polia sú nullable - null = bez filtra.
 */
@Data
@Builder
public class SearchCriteria {
    /** Regex pattern pre vyhľadávanie v názve transakcie */
    private String searchText;

    /** Filter podľa kategórie - null = všetky */
    private Integer categoryId;

    /** Filter podľa dátumu od */
    private LocalDateTime dateFrom;

    /** Filter podľa dátumu do */
    private LocalDateTime dateTo;

    /** Filter podľa sumy od */
    private Double amountFrom;

    /** Filter podľa sumy do */
    private Double amountTo;

    /** Filter podľa typu - null = všetky */
    private Integer transactionTypeId;

    private String categoryName;

    /**
     * Helper pre UI: nastaví filter na konkrétny dátum.
     * Nastaví dateFrom na začiatok dňa a dateTo na koniec dňa.
     */
    public void setExactDate(LocalDate date) {
        this.dateFrom = date.atStartOfDay();
        this.dateTo = date.atTime(23, 59, 59);
    }

    /**
     * Helper pre UI: nastaví filter na konkrétnu sumu.
     */
    public void setExactAmount(double amount) {
        this.amountFrom = amount;
        this.amountTo = amount;
    }
}