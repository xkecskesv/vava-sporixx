package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sk.sporixx.model.Category;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pre {@link CategoryService}.
 *
 * Pokrývame:
 *   - načítanie kategórií (systémové + vlastné)
 *   - pridanie novej kategórie (validácia, duplicita)
 *   - aktualizácia kategórie (system ochrana, duplicita)
 *   - vymazanie kategórie (system ochrana)
 */
@DisplayName("CategoryService – Management")
class ManagementCategoryServiceTest extends ManagementServiceTestSupport {

    // ======================== GET CATEGORIES ========================

    @Nested
    @DisplayName("Načítanie kategórií")
    class GetCategories {

        @Test
        @DisplayName("Prázdny repozitár → prázdny zoznam")
        void noCategories_returnsEmpty() {
            List<Category> result = categoryService.getCategories();
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Systémová kategória je viditeľná pre každého používateľa")
        void systemCategory_isVisible() {
            addSystemCategory("Food");
            List<Category> result = categoryService.getCategories();
            assertEquals(1, result.size());
            assertEquals("Food", result.get(0).getName());
        }

        @Test
        @DisplayName("Vlastná kategória je viditeľná len pre svojho vlastníka")
        void userCategory_isVisible() {
            addUserCategory("My Category");
            List<Category> result = categoryService.getCategories();
            assertEquals(1, result.size());
            assertEquals("My Category", result.get(0).getName());
        }

        @Test
        @DisplayName("Systémová aj vlastná kategória sa vrátia spolu")
        void systemAndUserCategories_returnedTogether() {
            addSystemCategory("Food");
            addUserCategory("My Category");
            List<Category> result = categoryService.getCategories();
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("Kategórie sú zoradené abecedne")
        void categories_sortedAlphabetically() {
            addSystemCategory("Rent");
            addSystemCategory("Food");
            addUserCategory("Clothing");
            List<Category> result = categoryService.getCategories();
            assertEquals("Clothing", result.get(0).getName());
            assertEquals("Food", result.get(1).getName());
            assertEquals("Rent", result.get(2).getName());
        }
    }

    // ======================== ADD CATEGORY ========================

    @Nested
    @DisplayName("Pridanie kategórie")
    class AddCategory {

        @Test
        @DisplayName("Platný názov → kategória sa uloží")
        void validName_categoryCreated() {
            categoryService.addCategory("Transport");
            List<Category> result = categoryService.getCategories();
            assertEquals(1, result.size());
            assertEquals("Transport", result.get(0).getName());
            assertEquals(1, result.get(0).getUserId()); // vlastník = prihlásenýuser
        }

        @Test
        @DisplayName("Prázdny názov → CategoryException")
        void blankName_throwsException() {
            assertThrows(CategoryException.class,
                    () -> categoryService.addCategory("   "));
        }

        @Test
        @DisplayName("Null názov → CategoryException")
        void nullName_throwsException() {
            assertThrows(CategoryException.class,
                    () -> categoryService.addCategory(null));
        }

        @Test
        @DisplayName("Duplicitný názov (case insensitive) → CategoryException")
        void duplicateName_throwsException() {
            categoryService.addCategory("Food");
            assertThrows(CategoryException.class,
                    () -> categoryService.addCategory("food"));
        }

        @Test
        @DisplayName("Duplicita voči systémovej kategórii → CategoryException")
        void duplicateWithSystemCategory_throwsException() {
            addSystemCategory("Rent");
            assertThrows(CategoryException.class,
                    () -> categoryService.addCategory("Rent"));
        }

        @Test
        @DisplayName("Vedúce a záverečné medzery sa orezávajú")
        void whitespaceIsTrimmed() {
            categoryService.addCategory("  Entertainment  ");
            List<Category> result = categoryService.getCategories();
            assertEquals("Entertainment", result.get(0).getName());
        }
    }

    // ======================== UPDATE CATEGORY ========================

    @Nested
    @DisplayName("Aktualizácia kategórie")
    class UpdateCategory {

        @Test
        @DisplayName("Platný nový názov → kategória sa aktualizuje")
        void validUpdate_categoryUpdated() {
            Category cat = addUserCategory("Old Name");
            categoryService.updateCategory(cat.getId(), "New Name");
            List<Category> result = categoryService.getCategories();
            assertEquals(1, result.size());
            assertEquals("New Name", result.get(0).getName());
        }

        @Test
        @DisplayName("Systémovú kategóriu nemožno upraviť → CategoryException")
        void systemCategory_cannotBeUpdated() {
            Category sys = addSystemCategory("Food");
            assertThrows(CategoryException.class,
                    () -> categoryService.updateCategory(sys.getId(), "New Food"));
        }

        @Test
        @DisplayName("Prázdny nový názov → CategoryException")
        void blankNewName_throwsException() {
            Category cat = addUserCategory("My Cat");
            assertThrows(CategoryException.class,
                    () -> categoryService.updateCategory(cat.getId(), ""));
        }

        @Test
        @DisplayName("Neexistujúce ID → CategoryException")
        void unknownId_throwsException() {
            assertThrows(CategoryException.class,
                    () -> categoryService.updateCategory(999, "Whatever"));
        }

        @Test
        @DisplayName("Duplicitný nový názov → CategoryException")
        void duplicateNewName_throwsException() {
            addUserCategory("Food");
            Category cat = addUserCategory("Transport");
            assertThrows(CategoryException.class,
                    () -> categoryService.updateCategory(cat.getId(), "Food"));
        }

        @Test
        @DisplayName("Rovnaký názov (self-update) je povolený")
        void sameNameSelfUpdate_allowed() {
            Category cat = addUserCategory("Transport");
            assertDoesNotThrow(() -> categoryService.updateCategory(cat.getId(), "Transport"));
        }

        @Test
        @DisplayName("Aktualizácia cudziej kategórie (iný userId) → CategoryException")
        void otherUserCategory_cannotBeUpdated() {
            // Kategória patriaca inému používateľovi
            Category other = Category.builder()
                    .userId(999)
                    .name("OtherUserCat")
                    .createdAt(java.time.LocalDateTime.now())
                    .build();
            categoryRepo.save(other);
            assertThrows(CategoryException.class,
                    () -> categoryService.updateCategory(other.getId(), "Hacked"));
        }

        @Test
        @DisplayName("Aktualizácia na rovnaký názov s inými medzerami je povolená")
        void updateWithTrimmedSameName_allowed() {
            Category cat = addUserCategory("Hobbies");
            assertDoesNotThrow(() -> categoryService.updateCategory(cat.getId(), "  Hobbies  "));
        }
    }

    // ======================== DELETE CATEGORY ========================

    @Nested
    @DisplayName("Vymazanie kategórie")
    class DeleteCategory {

        @Test
        @DisplayName("Vlastná kategória sa vymaže")
        void userCategory_deleted() {
            Category cat = addUserCategory("Hobbies");
            categoryService.deleteCategory(cat.getId());
            List<Category> result = categoryService.getCategories();
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Systémovú kategóriu nemožno vymazať → CategoryException")
        void systemCategory_cannotBeDeleted() {
            Category sys = addSystemCategory("Food");
            assertThrows(CategoryException.class,
                    () -> categoryService.deleteCategory(sys.getId()));
        }

        @Test
        @DisplayName("Neexistujúce ID → CategoryException")
        void unknownId_throwsException() {
            assertThrows(CategoryException.class,
                    () -> categoryService.deleteCategory(999));
        }

        @Test
        @DisplayName("Kategória používaná v transakcii → CategoryException")
        void categoryInUse_cannotBeDeleted() {
            Category cat = addUserCategory("UsedCat");
            // Pridáme transakciu s touto kategóriou
            transactionRepo.save(sk.sporixx.model.Transaction.builder()
                    .accountId(mainAccount.getId())
                    .categoryId(cat.getId())
                    .transactionTypeId(sk.sporixx.model.Transaction.TYPE_EXPENSE)
                    .amount(100.0).currencyCode("EUR")
                    .description("Test transaction")
                    .completeDate(java.time.LocalDateTime.now())
                    .createdAt(java.time.LocalDateTime.now())
                    .build());
            assertThrows(CategoryException.class,
                    () -> categoryService.deleteCategory(cat.getId()));
        }

        @Test
        @DisplayName("Cudzia kategória (iný userId) → CategoryException")
        void otherUserCategory_cannotBeDeleted() {
            Category other = Category.builder()
                    .userId(999)
                    .name("OtherCat")
                    .createdAt(java.time.LocalDateTime.now())
                    .build();
            categoryRepo.save(other);
            assertThrows(CategoryException.class,
                    () -> categoryService.deleteCategory(other.getId()));
        }
    }
}

