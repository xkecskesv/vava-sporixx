package sk.sporixx.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import sk.sporixx.model.*;
import sk.sporixx.service.testovanie.*;
import sk.sporixx.util.PasswordUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit testy pre ImportService.importSavingAccountsFromXml().
 *
 * Stratégia:
 *  - dočasné XML súbory sa vytvárajú v @BeforeEach a mažú v @AfterEach
 *  - AccountService sa stubuje anonymnou triedou (bez DB)
 *  - SessionManager sa prednastaví s testovacím userom a jeho účtami
 */
@DisplayName("ImportService – importSavingAccountsFromXml")
class ImportServiceTest {

    private InMemorySavingGoalRepository savingGoalRepo;
    private InMemoryTransactionRepository transactionRepo;
    private InMemoryAccountRepository accountRepo;
    private StubAccountService accountService;
    private ImportService importService;

    private User testUser;
    private Path tempXml;

    @BeforeEach
    void setUp() throws IOException {
        savingGoalRepo  = new InMemorySavingGoalRepository();
        transactionRepo = new InMemoryTransactionRepository();
        accountRepo     = new InMemoryAccountRepository();
        accountService  = new StubAccountService(accountRepo, savingGoalRepo);

        importService = new ImportServiceImpl(
                savingGoalRepo, accountService, transactionRepo, accountRepo);

        testUser = User.builder()
                .id(1).email("test@test.sk").firstName("Test").lastName("User")
                .passwordHash(PasswordUtil.hashPassword("Heslo123!"))
                .role(Role.USER).gender(GenderCode.UNKNOWN).isActive(true)
                .build();
    }

    @AfterEach
    void tearDown() throws IOException {
        SessionManager.getInstance().clearSession();
        if (tempXml != null) Files.deleteIfExists(tempXml);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    /** Nastaví session s daným zoznamom účtov */
    private void loginWithAccounts(List<Account> accounts) {
        SessionManager.getInstance().setSession(testUser, accounts);
    }

    /** Vytvorí dočasný XML súbor s daným obsahom */
    private Path writeXml(String content) throws IOException {
        Path p = Files.createTempFile("import-test-", ".xml");
        Files.writeString(p, content);
        return p;
    }

    /** Vytvorí saving account a uloží ho do accountRepo */
    private Account createSavingAccount(int id, String name, double balance) {
        Account a = Account.builder()
                .id(id).ownerUserId(testUser.getId())
                .accountTypeId(Account.SAVING_ACCOUNT)
                .description(name).currentBalance(balance)
                .initialBalance(0.0).regionId(1)
                .defaultCurrencyCode("EUR").isActive(true)
                .build();
        accountRepo.save(a);

        SavingGoal goal = SavingGoal.builder()
                .accountId(id).targetAmount(1000.0).currentAmount(balance)
                .isActive(true).build();
        savingGoalRepo.save(goal);
        return a;
    }

    /** Minimálny XML pre jeden saving account */
    private String minimalXml(String accountName, double savedUp, double target) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <SavingAccountsReport>
                  <SavingAccount name="%s" savedUp="%.2f" targetAmount="%.2f"
                    initialBalance="0.00"
                    targetDate="2027-01-01 00:00:00"
                    createdAt="2025-01-01 00:00:00"/>
                </SavingAccountsReport>
                """.formatted(accountName, savedUp, target);
    }

    /** XML s transakciami */
    private String xmlWithTransactions(String accountName, double savedUp, double target) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <SavingAccountsReport>
                  <SavingAccount name="%s" savedUp="%.2f" targetAmount="%.2f"
                    initialBalance="0.00"
                    targetDate="2027-01-01 00:00:00"
                    createdAt="2025-01-01 00:00:00">
                    <Transaction date="2025-06-01 10:00:00" amount="200.00"
                      description="Vklad" categoryId="0" currencyCode="EUR"/>
                    <Transaction date="2025-07-01 10:00:00" amount="300.00"
                      description="Ďalší vklad" categoryId="0" currencyCode="EUR"/>
                  </SavingAccount>
                </SavingAccountsReport>
                """.formatted(accountName, savedUp, target);
    }

    // ─── Validácia cesty ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Validácia cesty k súboru")
    class PathValidation {

        @Test
        @DisplayName("null cesta hodí ImportException")
        void import_nullPath_throws() {
            loginWithAccounts(List.of());
            ImportException ex = assertThrows(ImportException.class,
                    () -> importService.importSavingAccountsFromXml(null));
            assertEquals("import.error.invalid_path", ex.getMessageKey());
        }

        @Test
        @DisplayName("prázdna cesta hodí ImportException")
        void import_blankPath_throws() {
            loginWithAccounts(List.of());
            ImportException ex = assertThrows(ImportException.class,
                    () -> importService.importSavingAccountsFromXml(""));
            assertEquals("import.error.invalid_path", ex.getMessageKey());
        }

        @Test
        @DisplayName("neexistujúci súbor hodí ImportException")
        void import_nonExistentFile_throws() {
            loginWithAccounts(List.of());
            assertThrows(ImportException.class,
                    () -> importService.importSavingAccountsFromXml("/tmp/neexistuje_xyz.xml"));
        }
    }

    // ─── Formát XML ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Validácia XML formátu")
    class XmlFormat {

        @Test
        @DisplayName("XML s nesprávnym root elementom hodí ImportException")
        void import_wrongRootElement_throws() throws IOException {
            tempXml = writeXml("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <WrongRoot/>
                    """);
            loginWithAccounts(List.of());
            ImportException ex = assertThrows(ImportException.class,
                    () -> importService.importSavingAccountsFromXml(tempXml.toString()));
            assertEquals("import.error.invalid_xml_format", ex.getMessageKey());
        }

        @Test
        @DisplayName("prázdny SavingAccountsReport (0 účtov) sa spracuje bez chyby")
        void import_emptyReport_noError() throws IOException {
            tempXml = writeXml("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <SavingAccountsReport/>
                    """);
            loginWithAccounts(List.of());
            assertDoesNotThrow(() -> importService.importSavingAccountsFromXml(tempXml.toString()));
        }
    }

    // ─── Aktualizácia existujúceho účtu ──────────────────────────────────────

    @Nested
    @DisplayName("Aktualizácia existujúceho saving účtu")
    class UpdateExisting {

        @Test
        @DisplayName("existujúci účet sa NEcreate nový – zostatok sa aktualizuje")
        void import_existingAccount_updatesBalance() throws IOException {
            Account existing = createSavingAccount(1, "Dovolenka", 100.0);
            loginWithAccounts(List.of(existing));

            tempXml = writeXml(minimalXml("Dovolenka", 500.0, 1000.0));
            importService.importSavingAccountsFromXml(tempXml.toString());

            // createSavingAccountFromImport sa NESMIE volať
            assertEquals(0, accountService.createdCount,
                    "Nový účet sa nesmie vytvoriť pri existujúcom");
        }

        @Test
        @DisplayName("zostatok saving goalu sa aktualizuje na savedUp hodnotu z XML")
        void import_existingAccount_goalCurrentAmountUpdated() throws IOException {
            Account existing = createSavingAccount(1, "Dovolenka", 100.0);
            loginWithAccounts(List.of(existing));

            tempXml = writeXml(minimalXml("Dovolenka", 750.0, 1200.0));
            importService.importSavingAccountsFromXml(tempXml.toString());

            double updatedAmount = savingGoalRepo.findActiveByAccountId(1)
                    .getFirst().getCurrentAmount();
            assertEquals(750.0, updatedAmount, 0.001);
        }

        @Test
        @DisplayName("targetAmount saving goalu sa aktualizuje z XML")
        void import_existingAccount_goalTargetAmountUpdated() throws IOException {
            Account existing = createSavingAccount(1, "Dovolenka", 100.0);
            loginWithAccounts(List.of(existing));

            tempXml = writeXml(minimalXml("Dovolenka", 500.0, 1500.0));
            importService.importSavingAccountsFromXml(tempXml.toString());

            double updatedTarget = savingGoalRepo.findActiveByAccountId(1)
                    .getFirst().getTargetAmount();
            assertEquals(1500.0, updatedTarget, 0.001);
        }
    }

    // ─── Vytvorenie nového účtu ───────────────────────────────────────────────

    @Nested
    @DisplayName("Vytvorenie nového saving účtu")
    class CreateNew {

        @Test
        @DisplayName("neznámy účet v XML → vytvorí sa nový cez AccountService")
        void import_unknownAccount_createsNew() throws IOException {
            loginWithAccounts(List.of()); // žiadne existujúce saving účty

            tempXml = writeXml(minimalXml("Nový účet", 200.0, 800.0));
            importService.importSavingAccountsFromXml(tempXml.toString());

            assertEquals(1, accountService.createdCount, "Mal sa vytvoriť 1 nový účet");
        }

        @Test
        @DisplayName("správny názov sa odovzdá pri vytváraní nového účtu")
        void import_newAccount_correctNamePassed() throws IOException {
            loginWithAccounts(List.of());

            tempXml = writeXml(minimalXml("Špeciálny účet", 0.0, 500.0));
            importService.importSavingAccountsFromXml(tempXml.toString());

            assertEquals("Špeciálny účet", accountService.lastCreatedName);
        }
    }

    // ─── Import transakcií ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Import transakcií")
    class Transactions {

        @Test
        @DisplayName("transakcie z XML sa importujú do TransactionRepository")
        void import_withTransactions_savedToRepo() throws IOException {
            Account existing = createSavingAccount(1, "Sporenie", 0.0);
            loginWithAccounts(List.of(existing));

            tempXml = writeXml(xmlWithTransactions("Sporenie", 500.0, 1000.0));
            importService.importSavingAccountsFromXml(tempXml.toString());

            List<sk.sporixx.model.Transaction> saved = transactionRepo
                    .findByAccountIdAndDateRange(1,
                            LocalDateTime.of(2000, 1, 1, 0, 0),
                            LocalDateTime.now());
            assertEquals(2, saved.size());
        }

        @Test
        @DisplayName("existujúce TYPE_INCOME transakcie sa pred importom zmažú")
        void import_withTransactions_oldIncomeDeleted() throws IOException {
            Account existing = createSavingAccount(1, "Sporenie", 0.0);
            loginWithAccounts(List.of(existing));

            // vložíme starú transakciu
            transactionRepo.save(sk.sporixx.model.Transaction.builder()
                    .accountId(1).transactionTypeId(sk.sporixx.model.Transaction.TYPE_INCOME)
                    .amount(50.0).currencyCode("EUR").completeDate(LocalDateTime.now())
                    .createdAt(LocalDateTime.now()).build());

            tempXml = writeXml(xmlWithTransactions("Sporenie", 500.0, 1000.0));
            importService.importSavingAccountsFromXml(tempXml.toString());

            // starých 1 + nových 2 = 2 (stará sa zmazala)
            List<sk.sporixx.model.Transaction> saved = transactionRepo
                    .findByAccountIdAndDateRange(1,
                            LocalDateTime.of(2000, 1, 1, 0, 0),
                            LocalDateTime.now());
            assertEquals(2, saved.size());
        }

        @Test
        @DisplayName("transakcia s amount <= 0 sa neimportuje")
        void import_zeroAmountTransaction_skipped() throws IOException {
            Account existing = createSavingAccount(1, "Sporenie", 0.0);
            loginWithAccounts(List.of(existing));

            tempXml = writeXml("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <SavingAccountsReport>
                      <SavingAccount name="Sporenie" savedUp="0.00" targetAmount="1000.00"
                        initialBalance="0.00" targetDate="2027-01-01 00:00:00"
                        createdAt="2025-01-01 00:00:00">
                        <Transaction date="2025-06-01 10:00:00" amount="0.00"
                          description="Nulový vklad" categoryId="0" currencyCode="EUR"/>
                      </SavingAccount>
                    </SavingAccountsReport>
                    """);
            importService.importSavingAccountsFromXml(tempXml.toString());

            List<sk.sporixx.model.Transaction> saved = transactionRepo
                    .findByAccountIdAndDateRange(1,
                            LocalDateTime.of(2000, 1, 1, 0, 0),
                            LocalDateTime.now());
            assertTrue(saved.isEmpty());
        }

        @Test
        @DisplayName("chýbajúce currencyCode v transakcii → defaultne EUR")
        void import_missingCurrencyCode_defaultsToEur() throws IOException {
            Account existing = createSavingAccount(1, "Sporenie", 0.0);
            loginWithAccounts(List.of(existing));

            tempXml = writeXml("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <SavingAccountsReport>
                      <SavingAccount name="Sporenie" savedUp="100.00" targetAmount="1000.00"
                        initialBalance="0.00" targetDate="2027-01-01 00:00:00"
                        createdAt="2025-01-01 00:00:00">
                        <Transaction date="2025-06-01 10:00:00" amount="100.00"
                          description="Vklad" categoryId="0" currencyCode=""/>
                      </SavingAccount>
                    </SavingAccountsReport>
                    """);
            importService.importSavingAccountsFromXml(tempXml.toString());

            sk.sporixx.model.Transaction saved = transactionRepo
                    .findByAccountIdAndDateRange(1,
                            LocalDateTime.of(2000, 1, 1, 0, 0),
                            LocalDateTime.now()).getFirst();
            assertEquals("EUR", saved.getCurrencyCode());
        }
    }

    // ─── Viacero účtov ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Viacero účtov v XML")
    class MultipleAccounts {

        @Test
        @DisplayName("XML s dvoma účtami – jeden existujúci, jeden nový")
        void import_mixedAccounts_updatesAndCreates() throws IOException {
            Account existing = createSavingAccount(1, "Dovolenka", 100.0);
            loginWithAccounts(List.of(existing));

            tempXml = writeXml("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <SavingAccountsReport>
                      <SavingAccount name="Dovolenka" savedUp="500.00" targetAmount="1000.00"
                        initialBalance="0.00" targetDate="2027-01-01 00:00:00"
                        createdAt="2025-01-01 00:00:00"/>
                      <SavingAccount name="Auto" savedUp="200.00" targetAmount="5000.00"
                        initialBalance="0.00" targetDate="2028-01-01 00:00:00"
                        createdAt="2025-01-01 00:00:00"/>
                    </SavingAccountsReport>
                    """);
            importService.importSavingAccountsFromXml(tempXml.toString());

            assertEquals(1, accountService.createdCount, "Len 1 nový účet (Auto)");
        }
    }

    // ─── Stub AccountService ──────────────────────────────────────────────────

    /**
     * Stub AccountService – zachytáva volania createSavingAccountFromImport
     * a vytvára účet v InMemoryAccountRepository.
     */
    private static class StubAccountService implements AccountService {

        final InMemoryAccountRepository accountRepo;
        final InMemorySavingGoalRepository savingGoalRepo;
        int createdCount = 0;
        String lastCreatedName = null;
        private int nextId = 100;

        StubAccountService(InMemoryAccountRepository accountRepo,
                           InMemorySavingGoalRepository savingGoalRepo) {
            this.accountRepo = accountRepo;
            this.savingGoalRepo = savingGoalRepo;
        }

        @Override
        public Account createSavingAccountFromImport(String description, double initialAmount,
                                                      double targetAmount, LocalDate targetDate,
                                                      LocalDateTime createdAt) {
            createdCount++;
            lastCreatedName = description;
            Account a = Account.builder()
                    .id(nextId++).ownerUserId(1)
                    .accountTypeId(Account.SAVING_ACCOUNT)
                    .description(description).currentBalance(initialAmount)
                    .initialBalance(initialAmount).regionId(1)
                    .defaultCurrencyCode("EUR").isActive(true)
                    .build();
            accountRepo.save(a);
            SavingGoal goal = SavingGoal.builder()
                    .accountId(a.getId()).targetAmount(targetAmount)
                    .currentAmount(initialAmount).isActive(true).build();
            savingGoalRepo.save(goal);
            return a;
        }

        // ostatné metódy – no-op
        @Override public void createPrivateAccount(String d, double a) {}
        @Override public void createSavingAccount(String d, double i, double t, LocalDate td) {}
        @Override public void deleteAccount(int id) {}
        @Override public void updateAccountDescription(int id, String d) {}
        @Override public void updateSavingAccount(int id, String d, double t, LocalDate td) {}
        @Override public java.util.Optional<SavingGoal> getSavingGoal(int accountId) {
            return savingGoalRepo.findActiveByAccountId(accountId).stream().findFirst();
        }
        @Override public String getLocalizedDescription(Account account) { return account.getDescription(); }
    }
}


