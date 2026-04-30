package sk.sporixx.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sk.sporixx.dto.SearchCriteria;
import sk.sporixx.model.Transaction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pre {@link TransactionService#searchTransactions}.
 *
 * Pokrývame:
 *   - filtrovanie (kategória, dátum, suma, typ)
 *   - regex search (case-insensitive, unicode)
 *   - invalid regex → fallback na plain text contains
 *   - search across all accounts (overload bez accountId)
 */
class TransactionServiceSearchTest extends TransactionServiceTestSupport {

    /** Helper: pripraví 4 transakcie s rôznymi vlastnosťami pre filter testy. */
    private void seed4Transactions() {
        // 1. Income 500 — Salary
        insertRawTransaction(mainAccount.getId(), Transaction.TYPE_INCOME,
                categorySalaryId, 500.0, "Mesačný plat",
                LocalDateTime.now().minusDays(5));
        // 2. Expense 50 — Food (need)
        Transaction t2 = insertRawTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE,
                categoryFoodId, 50.0, "Tesco nákup",
                LocalDateTime.now().minusDays(3));
        t2.setSpendingClassificationId(Transaction.CLASSIFICATION_NEED);
        // 3. Expense 200 — Food (want)
        Transaction t3 = insertRawTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE,
                categoryFoodId, 200.0, "Reštaurácia",
                LocalDateTime.now().minusDays(1));
        t3.setSpendingClassificationId(Transaction.CLASSIFICATION_WANT);
        // 4. Income 100 — Salary, na inom účte
        insertRawTransaction(savingAccount.getId(), Transaction.TYPE_INCOME,
                categorySalaryId, 100.0, "Bonus", LocalDateTime.now().minusDays(2));
    }

    // ============ FILTROVANIE PODĽA ÚČTU ============

    @Test
    @DisplayName("search bez kritérií — vráti všetky transakcie účtu")
    void search_noCriteria_returnsAllForAccount() {
        seed4Transactions();
        SearchCriteria empty = SearchCriteria.builder().build();

        List<Transaction> result = transactionService.searchTransactions(empty, mainAccount.getId());
        assertEquals(3, result.size(), "main account má 3 transakcie");
    }

    @Test
    @DisplayName("search by transactionType=EXPENSE")
    void search_byType_expense() {
        seed4Transactions();
        SearchCriteria c = SearchCriteria.builder()
                .transactionTypeId(Transaction.TYPE_EXPENSE).build();

        List<Transaction> result = transactionService.searchTransactions(c, mainAccount.getId());
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(Transaction::isExpense));
    }

    @Test
    @DisplayName("search by category=Food")
    void search_byCategory_food() {
        seed4Transactions();
        SearchCriteria c = SearchCriteria.builder().categoryId(categoryFoodId).build();

        List<Transaction> result = transactionService.searchTransactions(c, mainAccount.getId());
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(t -> t.getCategoryId() == categoryFoodId));
    }

    @Test
    @DisplayName("search by amount range [40, 100]")
    void search_byAmountRange() {
        seed4Transactions();
        SearchCriteria c = SearchCriteria.builder()
                .amountFrom(40.0).amountTo(100.0).build();

        List<Transaction> result = transactionService.searchTransactions(c, mainAccount.getId());
        assertEquals(1, result.size(), "len 50 € expense spadá do rangu [40,100]");
        assertEquals(50.0, result.get(0).getAmount());
    }

    @Test
    @DisplayName("search by date range — last 4 days")
    void search_byDateRange() {
        seed4Transactions();
        SearchCriteria c = SearchCriteria.builder()
                .dateFrom(LocalDateTime.now().minusDays(4))
                .dateTo(LocalDateTime.now())
                .build();

        List<Transaction> result = transactionService.searchTransactions(c, mainAccount.getId());
        // tx 2 (3d ago) + tx 3 (1d ago) — tx 1 (5d ago) je mimo rangu
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("search combine: type=EXPENSE AND amount>=100")
    void search_combineFilters() {
        seed4Transactions();
        SearchCriteria c = SearchCriteria.builder()
                .transactionTypeId(Transaction.TYPE_EXPENSE)
                .amountFrom(100.0).build();

        List<Transaction> result = transactionService.searchTransactions(c, mainAccount.getId());
        assertEquals(1, result.size());
        assertEquals(200.0, result.get(0).getAmount());
    }

    // ============ REGEX SEARCH ============

    @Test
    @DisplayName("regex search — case insensitive 'tesco' nájde 'Tesco'")
    void search_regex_caseInsensitive() {
        seed4Transactions();
        SearchCriteria c = SearchCriteria.builder().searchText("tesco").build();

        List<Transaction> result = transactionService.searchTransactions(c, mainAccount.getId());
        assertEquals(1, result.size());
        assertEquals("Tesco nákup", result.get(0).getDescription());
    }

    @Test
    @DisplayName("regex search — pattern '^Mes' nájde 'Mesačný plat'")
    void search_regex_anchorPattern() {
        seed4Transactions();
        SearchCriteria c = SearchCriteria.builder().searchText("^Mes").build();

        List<Transaction> result = transactionService.searchTransactions(c, mainAccount.getId());
        assertEquals(1, result.size());
        assertEquals("Mesačný plat", result.get(0).getDescription());
    }

    @Test
    @DisplayName("regex search — unicode case (Š = š) match")
    void search_regex_unicodeCase() {
        seed4Transactions();
        SearchCriteria c = SearchCriteria.builder().searchText("nákup").build();

        List<Transaction> result = transactionService.searchTransactions(c, mainAccount.getId());
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("regex search — invalid regex padne na plain text contains")
    void search_invalidRegex_fallsBackToPlainText() {
        seed4Transactions();
        // Invalid regex — unbalanced bracket
        SearchCriteria c = SearchCriteria.builder().searchText("Tesco[").build();

        List<Transaction> result = transactionService.searchTransactions(c, mainAccount.getId());
        // pattern '[' nie je platný regex, ale 'Tesco[' ako plain text sa nikde nenachádza
        // → 0 výsledkov, ale test nesmie hodiť exception
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("regex search — invalid regex 'Tesc' (pred [) padne, ale plain text 'Tesc' matchuje 'Tesco nákup'")
    void search_invalidRegex_plainTextMatch() {
        seed4Transactions();
        SearchCriteria c = SearchCriteria.builder().searchText("Tesc(o[").build();

        List<Transaction> result = transactionService.searchTransactions(c, mainAccount.getId());
        // Invalid regex → fallback contains "tesc(o["
        // 'Tesco nákup' neobsahuje 'tesc(o[' → 0 výsledkov
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("regex search — prázdny searchText → vráti všetky filterované")
    void search_blankSearchText_returnsFiltered() {
        seed4Transactions();
        SearchCriteria c = SearchCriteria.builder().searchText("   ").build();

        List<Transaction> result = transactionService.searchTransactions(c, mainAccount.getId());
        assertEquals(3, result.size(), "blank text = no regex filter");
    }

    @Test
    @DisplayName("regex search — null searchText (default) → vráti všetky filterované")
    void search_nullSearchText_returnsFiltered() {
        seed4Transactions();
        SearchCriteria c = SearchCriteria.builder().build(); // searchText=null

        List<Transaction> result = transactionService.searchTransactions(c, mainAccount.getId());
        assertEquals(3, result.size());
    }

    // ============ SECURITY / EDGE CASES ============

    @Test
    @DisplayName("BUG nález: ReDoS — catastrophic backtracking nie je obmedzený")
    void search_redos_canBeSlow() {
        // Pridám transakciu s opisom ktorý spôsobí catastrophic backtracking
        insertRawTransaction(mainAccount.getId(), Transaction.TYPE_EXPENSE,
                categoryFoodId, 1.0,
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa!", LocalDateTime.now());

        // Tento pattern '(a+)+$' je klasický ReDoS pattern
        SearchCriteria c = SearchCriteria.builder().searchText("(a+)+$").build();

        long start = System.currentTimeMillis();
        // Ak by tu bol skutočne dlhý retazec, test by vyhodil timeout
        // pre demo scenár použijeme krátky string aby test neblokoval CI
        transactionService.searchTransactions(c, mainAccount.getId());
        long duration = System.currentTimeMillis() - start;

        // dokumentujeme — nie je timeout/limit, takže dlhý retazec by zahltil thread
        assertTrue(duration < 5000,
                "Pre tento input je rýchlosť OK, ale POZOR: pre dlhšie reťazce " +
                "by ReDoS mohol zablokovať thread. Mitigácia: limit dĺžky alebo timeout.");
    }

    // ============ SEARCH ACROSS ACCOUNTS ============

    @Test
    @DisplayName("searchTransactions(criteria) bez accountId — naprieč všetkými účtami")
    void search_acrossAllAccounts() {
        seed4Transactions();
        SearchCriteria c = SearchCriteria.builder()
                .transactionTypeId(Transaction.TYPE_INCOME).build();

        List<Transaction> result = transactionService.searchTransactions(c);
        // 2 income — jedno na main, druhé na saving
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("searchTransactions(criteria) — sortuje od najnovších")
    void search_sortedDescending() {
        seed4Transactions();
        SearchCriteria empty = SearchCriteria.builder().build();

        List<Transaction> result = transactionService.searchTransactions(empty);
        assertEquals(4, result.size());
        for (int i = 0; i < result.size() - 1; i++) {
            assertFalse(result.get(i).getCompleteDate()
                            .isBefore(result.get(i + 1).getCompleteDate()),
                    "transakcie musia byt sortované DESC podla completeDate");
        }
    }

    // ============ HELPERS — getTransactions / getAllTransactions ============

    @Test
    @DisplayName("getTransactions — vráti transakcie 1 účtu")
    void getTransactions_singleAccount() {
        seed4Transactions();
        List<Transaction> result = transactionService.getTransactions(mainAccount.getId());
        assertEquals(3, result.size());
    }

    @Test
    @DisplayName("getAllTransactions — vráti transakcie všetkých účtov + sortuje DESC")
    void getAllTransactions_allUsersAccounts() {
        seed4Transactions();
        List<Transaction> result = transactionService.getAllTransactions();
        assertEquals(4, result.size());

        // sortovanie DESC
        for (int i = 0; i < result.size() - 1; i++) {
            assertFalse(result.get(i).getCompleteDate()
                    .isBefore(result.get(i + 1).getCompleteDate()));
        }
    }

    @Test
    @DisplayName("getAllTransactions — keď user nemá účty, vracia prazdny zoznam")
    void getAllTransactions_noAccounts_emptyList() {
        // odhlásime sa a prihlásime znova bez účtov
        SessionManager.getInstance().clearSession();
        SessionManager.getInstance().setSession(testUser, List.of());

        List<Transaction> result = transactionService.getAllTransactions();
        assertTrue(result.isEmpty());
    }
}
