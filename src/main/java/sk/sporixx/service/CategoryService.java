package sk.sporixx.service;

import sk.sporixx.model.Category;
import java.util.List;

/**
 * Service pre správu kategórií transakcií.
 * Kategórie môžu byť:
 * - systémové (userId = null): predpripravené pre všetkých používateľov
 * - používateľské (userId = konkrétny user): vlastné kategórie
 */
public interface CategoryService {

    /**
     * Načíta všetky kategórie dostupné pre prihláseného používateľa.
     * Vracia systémové (userId = null) + jeho vlastné.
     * Používa sa pri načítaní dropdownu kategórií vo formulári transakcie.
     */
    List<Category> getCategories();

    /**
     * Načíta kategórie dostupné používateľovi pri pridávaní/editácii transakcie.
     * Vylučuje systémové kategórie Saving a Saving Expense — tie sa priraďujú
     * automaticky pri transferoch medzi účtami.
     * Investment je zahrnutý — používateľ si ho môže vybrať manuálne.
     */
    List<Category> getSelectableCategories();

    /**
     * Pridá novú vlastnú kategóriu pre prihláseného používateľa.
     * Validuje, že názov nie je prázdny a neexistuje duplicita (case-insensitive).
     */
    Category addCategory(String name);

    /**
     * Aktualizuje názov existujúcej kategórie.
     * Systémové kategórie sa nedajú upravovať.
     * Validuje, že nový názov nie je prázdny a neexistuje duplicita.
     */
    void updateCategory(int categoryId, String newName);

    /**
     * Vymaže vlastnú kategóriu prihláseného používateľa.
     * Systémové kategórie sa nedajú vymazať.
     */
    void deleteCategory(int categoryId);
}
