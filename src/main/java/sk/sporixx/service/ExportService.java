package sk.sporixx.service;

import sk.sporixx.dto.ChartPeriod;

/**
 * Service pre export reportov do XML súboru.
 * Používa javax.xml pre generovanie XML.
 * UI poskytuje filePath cez JavaFX FileChooser.
 */
public interface ExportService {

    /**
     * Exportuje Income & Expenses report do XML.
     * Grouping (deň/mesiac) závisí od zvoleného ChartPeriod.
     * @param period zvolené časové obdobie (ONE_WEEK, ONE_MONTH, SIX_MONTHS, TWELVE_MONTHS)
     * @param filePath absolútna cesta kam uložiť súbor — používateľ vyberá cez FileChooser v UI
     */
    void exportIncomeExpenseToXml(ChartPeriod period, String filePath);

    /**
     * Exportuje Saving Accounts report do XML.
     * Exportuje aktuálny stav všetkých saving goalov.
     * @param filePath absolútna cesta kam uložiť súbor
     */
    void exportSavingAccountsToXml(String filePath);
}