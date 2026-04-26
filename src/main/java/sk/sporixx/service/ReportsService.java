package sk.sporixx.service;

import sk.sporixx.dto.*;

import java.util.List;

/**
 * Service rozhranie pre Reports obrazovku.
 * Všetky metódy pracujú s dátami naprieč všetkými účtami používateľa.
 * Prevody medzi účtami (TYPE_SAVING, TYPE_SAVING_EXPENSE) sú vynechané.
 */
public interface ReportsService {

    /**
     * Načíta dáta pre Income vs Expenses líniový graf.
     * @param period počet mesiacov/týždňov dozadu (zvyčajne 12)
     */
    IncomeExpenseData loadIncomeExpenseData(ChartPeriod period);

    /**
     * Načíta dáta pre Expenses by Category koláčový graf.
     * @param period počet mesiacov/týždňov dozadu
     */
    CategoryExpenseData loadCategoryExpenseData(ChartPeriod period);

    /**
     * Načíta dáta pre Recurring Expenses stĺpcový graf.
     * Obsahuje všetky aktívne recurring rules naprieč všetkými účtami.
     */
    RecurringExpenseData loadRecurringExpenseData();

    /**
     * Načíta dáta pre Want vs Need stĺpcový graf.
     * @param period počet mesiacov/týždňov dozadu
     */
    WantNeedData loadWantNeedData(ChartPeriod period);

    /**
     * Načíta dáta pre Saving Accounts screen.
     * Vracia zoznam — jeden záznam pre každý saving účet.
     */
    List<SavingAccountReportData> loadSavingAccountsData();
}