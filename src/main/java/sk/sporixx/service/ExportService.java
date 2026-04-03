package sk.sporixx.service;

/**
 * Service pre export reportov do XML súboru.
 * Používa javax.xml pre generovanie XML.
 * UI poskytuje filePath cez JavaFX FileChooser.
 */
public interface ExportService {

    /**
     * Exportuje Income & Expenses report do XML.
     * Načíta dáta cez ReportsService a vygeneruje XML súbor.
     * @param months počet mesiacov dozadu
     * @param filePath absolútna cesta kam uložiť súbor
     */
    void exportIncomeExpenseToXml(int months, String filePath);

    /**
     * Exportuje Saving Accounts report do XML.
     * Exportuje aktuálny stav všetkých saving goalov.
     * @param filePath absolútna cesta kam uložiť súbor
     */
    void exportSavingAccountsToXml(String filePath);
}