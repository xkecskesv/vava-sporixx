package sk.sporixx.repository;

import sk.sporixx.model.Category;
import java.util.List;
import java.util.Optional;

/**
 * Repository rozhranie pre prístup ku kategóriám transakcií.
 */
public interface CategoryRepository {

    /**
     * Vráti všetky kategórie dostupné pre používateľa —
     * systémové (userId = null) + jeho vlastné (userId = dané ID).
     * Volá sa pri načítaní formulára pre novú transakciu.
     */
    List<Category> findByUserIdOrSystem(int userId);

    /**
     * Nájde kategóriu podľa ID.
     */
    Optional<Category> findById(int categoryId);

    /**
     * Uloží novú kategóriu.
     * Volá sa keď používateľ vytvorí vlastnú kategóriu v Management screene.
     */
    Category save(Category category);

    /**
     * Vymaže kategóriu podľa ID.
     * service vrstva musí overiť, že nejde o systémovú kategóriu.
     */
    void deleteById(int categoryId);
}