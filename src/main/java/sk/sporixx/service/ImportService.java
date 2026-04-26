package sk.sporixx.service;

/**
 * Service pre import dát z XML súboru.
 */
public interface ImportService {
    /**
     * Importuje Saving Accounts dáta z XML súboru.
     * @param filePath absolútna cesta k XML súboru
     */
    void importSavingAccountsFromXml(String filePath);
}
