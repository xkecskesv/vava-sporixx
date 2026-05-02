package sk.sporixx.service;

import org.junit.jupiter.api.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import sk.sporixx.dto.ChartPeriod;
import sk.sporixx.dto.IncomeExpenseData;
import sk.sporixx.dto.SavingAccountReportData;
import sk.sporixx.model.Transaction;
import sk.sporixx.util.XmlUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pre ExportService.exportSavingAccountsToXml().
 */
@DisplayName("ExportService – exportSavingAccountsToXml()")
class ExportServiceSavingAccountsTest {

    private FakeReportsService fakeReports;
    private ExportService exportService;
    private Path tempFile;

    @BeforeEach
    void setUp() throws Exception {
        fakeReports = new FakeReportsService();
        exportService = new ExportServiceImpl(fakeReports);
        tempFile = Files.createTempFile("export-saving-test-", ".xml");
    }

    @AfterEach
    void tearDown() throws Exception {
        Files.deleteIfExists(tempFile);
    }

    // ====================== VALIDÁCIA VSTUPU ======================

    @Test
    @DisplayName("null filePath → ExportException s kľúčom invalid_path")
    void nullPath_throwsExportException() {
        ExportException ex = assertThrows(ExportException.class,
                () -> exportService.exportSavingAccountsToXml(null));
        assertEquals("export.error.invalid_path", ex.getMessageKey());
    }

    @Test
    @DisplayName("prázdny filePath → ExportException s kľúčom invalid_path")
    void blankPath_throwsExportException() {
        ExportException ex = assertThrows(ExportException.class,
                () -> exportService.exportSavingAccountsToXml("   "));
        assertEquals("export.error.invalid_path", ex.getMessageKey());
    }

    @Test
    @DisplayName("žiadne saving accounts → ExportException s kľúčom no_saving_accounts")
    void noSavingAccounts_throwsExportException() {
        fakeReports.setSavingAccountsData(List.of());
        ExportException ex = assertThrows(ExportException.class,
                () -> exportService.exportSavingAccountsToXml(tempFile.toString()));
        assertEquals("export.error.no_saving_accounts", ex.getMessageKey());
    }

    // ====================== ŠTRUKTÚRA XML ======================

    @Test
    @DisplayName("exportovaný súbor existuje a nie je prázdny")
    void exportCreatesFile() throws Exception {
        fakeReports.setSavingAccountsData(List.of(simpleSavingAccount(1, "Dovolenka", 500, 1500)));
        exportService.exportSavingAccountsToXml(tempFile.toString());
        assertTrue(Files.exists(tempFile));
        assertTrue(Files.size(tempFile) > 0);
    }

    @Test
    @DisplayName("root element je SavingAccountsReport")
    void rootElementName() throws Exception {
        fakeReports.setSavingAccountsData(List.of(simpleSavingAccount(1, "Auto", 1000, 5000)));
        exportService.exportSavingAccountsToXml(tempFile.toString());
        Document doc = parseXml(tempFile);
        assertEquals("SavingAccountsReport", doc.getDocumentElement().getTagName());
    }

    @Test
    @DisplayName("root obsahuje atribút accountCount zodpovedajúci počtu účtov")
    void rootHasAccountCount() throws Exception {
        fakeReports.setSavingAccountsData(List.of(
                simpleSavingAccount(1, "Dovolenka", 500, 1500),
                simpleSavingAccount(2, "Auto", 2000, 8000)));
        exportService.exportSavingAccountsToXml(tempFile.toString());
        Document doc = parseXml(tempFile);
        assertEquals("2", doc.getDocumentElement().getAttribute("accountCount"));
    }

    @Test
    @DisplayName("root obsahuje atribút exportedAt (nie je prázdny)")
    void rootHasExportedAt() throws Exception {
        fakeReports.setSavingAccountsData(List.of(simpleSavingAccount(1, "Test", 100, 500)));
        exportService.exportSavingAccountsToXml(tempFile.toString());
        Document doc = parseXml(tempFile);
        assertFalse(doc.getDocumentElement().getAttribute("exportedAt").isBlank());
    }

    // ====================== SAVING ACCOUNT ELEMENT ======================

    @Test
    @DisplayName("SavingAccount element má správne základné atribúty")
    void savingAccountElement_basicAttributes() throws Exception {
        fakeReports.setSavingAccountsData(List.of(simpleSavingAccount(42, "Dovolenka", 500, 2000)));
        exportService.exportSavingAccountsToXml(tempFile.toString());
        Document doc = parseXml(tempFile);
        Element account = (Element) doc.getElementsByTagName("SavingAccount").item(0);
        assertNotNull(account);
        assertEquals("42", account.getAttribute("id"));
        assertEquals("Dovolenka", account.getAttribute("name"));
        assertEquals("500.0", account.getAttribute("savedUp"));
        assertEquals("1500.0", account.getAttribute("needToSave"));
        assertEquals("2000.0", account.getAttribute("targetAmount"));
    }

    @Test
    @DisplayName("Správny počet SavingAccount elementov pri viacerých účtoch")
    void multipleSavingAccounts_correctCount() throws Exception {
        fakeReports.setSavingAccountsData(List.of(
                simpleSavingAccount(1, "Dovolenka", 500, 1500),
                simpleSavingAccount(2, "Auto", 2000, 8000),
                simpleSavingAccount(3, "Byt", 10000, 50000)));
        exportService.exportSavingAccountsToXml(tempFile.toString());
        Document doc = parseXml(tempFile);
        NodeList accounts = doc.getElementsByTagName("SavingAccount");
        assertEquals(3, accounts.getLength());
    }

    @Test
    @DisplayName("targetDate a createdAt sú prázdny string keď sú null")
    void targetDate_nullExportsEmptyString() throws Exception {
        SavingAccountReportData account = SavingAccountReportData.builder()
                .accountId(1).accountName("Test")
                .savedUp(100).needToSave(400).targetAmount(500)
                .targetDate(null).initialBalance(0).createdAt(null)
                .progressGrouping("MONTH")
                .expectedProgress(Map.of()).actualProgress(Map.of())
                .transactions(List.of()).build();
        fakeReports.setSavingAccountsData(List.of(account));
        exportService.exportSavingAccountsToXml(tempFile.toString());
        Document doc = parseXml(tempFile);
        Element el = (Element) doc.getElementsByTagName("SavingAccount").item(0);
        assertEquals("", el.getAttribute("targetDate"));
        assertEquals("", el.getAttribute("createdAt"));
    }

    // ====================== EXPECTED / ACTUAL PROGRESS ======================

    @Test
    @DisplayName("ExpectedProgress obsahuje správny počet Point elementov")
    void expectedProgress_correctPointCount() throws Exception {
        Map<String, Double> expected = Map.of("2026-01", 200.0, "2026-02", 400.0);
        fakeReports.setSavingAccountsData(List.of(savingAccountWithProgress(expected, Map.of())));
        exportService.exportSavingAccountsToXml(tempFile.toString());
        Document doc = parseXml(tempFile);
        Element expectedEl = (Element) doc.getElementsByTagName("ExpectedProgress").item(0);
        assertEquals(2, expectedEl.getElementsByTagName("Point").getLength());
    }

    @Test
    @DisplayName("ActualProgress obsahuje správny počet Point elementov")
    void actualProgress_correctPointCount() throws Exception {
        Map<String, Double> actual = Map.of("2026-01", 150.0, "2026-02", 320.0, "2026-03", 500.0);
        fakeReports.setSavingAccountsData(List.of(savingAccountWithProgress(Map.of(), actual)));
        exportService.exportSavingAccountsToXml(tempFile.toString());
        Document doc = parseXml(tempFile);
        Element actualEl = (Element) doc.getElementsByTagName("ActualProgress").item(0);
        assertEquals(3, actualEl.getElementsByTagName("Point").getLength());
    }

    // ====================== TRANSACTIONS ======================

    @Test
    @DisplayName("Transactions element obsahuje správny počet Transaction elementov")
    void transactions_correctCount() throws Exception {
        LocalDateTime date = LocalDateTime.of(2026, 3, 15, 10, 0);
        List<Transaction> txs = List.of(
                Transaction.builder().accountId(1).amount(100.0).completeDate(date)
                        .description("vklad 1").categoryId(null).currencyCode("EUR").build(),
                Transaction.builder().accountId(1).amount(200.0).completeDate(date)
                        .description("vklad 2").categoryId(null).currencyCode("EUR").build());
        SavingAccountReportData account = SavingAccountReportData.builder()
                .accountId(1).accountName("Test")
                .savedUp(300).needToSave(200).targetAmount(500)
                .targetDate(null).initialBalance(0).createdAt(null)
                .progressGrouping("MONTH")
                .expectedProgress(Map.of()).actualProgress(Map.of())
                .transactions(txs).build();
        fakeReports.setSavingAccountsData(List.of(account));
        exportService.exportSavingAccountsToXml(tempFile.toString());
        Document doc = parseXml(tempFile);
        Element transactionsEl = (Element) doc.getElementsByTagName("Transactions").item(0);
        assertEquals(2, transactionsEl.getElementsByTagName("Transaction").getLength());
    }

    @Test
    @DisplayName("Transakcia má správne atribúty amount, currencyCode a description")
    void transaction_correctAttributes() throws Exception {
        LocalDateTime date = LocalDateTime.of(2026, 3, 15, 10, 0);
        Transaction tx = Transaction.builder()
                .accountId(1).amount(350.0).completeDate(date)
                .description("sporenie").categoryId(null).currencyCode("EUR").build();
        SavingAccountReportData account = SavingAccountReportData.builder()
                .accountId(1).accountName("Test")
                .savedUp(350).needToSave(150).targetAmount(500)
                .targetDate(null).initialBalance(0).createdAt(null)
                .progressGrouping("MONTH")
                .expectedProgress(Map.of()).actualProgress(Map.of())
                .transactions(List.of(tx)).build();
        fakeReports.setSavingAccountsData(List.of(account));
        exportService.exportSavingAccountsToXml(tempFile.toString());
        Document doc = parseXml(tempFile);
        Element txEl = (Element) doc.getElementsByTagName("Transaction").item(0);
        assertEquals("350.0", txEl.getAttribute("amount"));
        assertEquals("EUR", txEl.getAttribute("currencyCode"));
        assertEquals("sporenie", txEl.getAttribute("description"));
    }

    @Test
    @DisplayName("Prázdny zoznam transakcií → Transactions element bez potomkov")
    void emptyTransactions_noChildren() throws Exception {
        SavingAccountReportData account = SavingAccountReportData.builder()
                .accountId(1).accountName("Test")
                .savedUp(100).needToSave(400).targetAmount(500)
                .targetDate(null).initialBalance(0).createdAt(null)
                .progressGrouping("MONTH")
                .expectedProgress(Map.of()).actualProgress(Map.of())
                .transactions(List.of()).build();
        fakeReports.setSavingAccountsData(List.of(account));
        exportService.exportSavingAccountsToXml(tempFile.toString());
        Document doc = parseXml(tempFile);
        Element transactionsEl = (Element) doc.getElementsByTagName("Transactions").item(0);
        assertEquals(0, transactionsEl.getElementsByTagName("Transaction").getLength());
    }

    // ====================== CHYBOVÉ SCENÁRE ======================

    @Test
    @DisplayName("ReportsService hodí výnimku → ExportException s kľúčom failed")
    void reportsServiceException_throwsExportException() {
        fakeReports.setThrowOnSavingAccounts(true);
        ExportException ex = assertThrows(ExportException.class,
                () -> exportService.exportSavingAccountsToXml(tempFile.toString()));
        assertEquals("export.error.failed", ex.getMessageKey());
    }

    @Test
    @DisplayName("neplatná cesta k súboru → ExportException s kľúčom failed")
    void invalidDirectory_throwsExportException() {
        fakeReports.setSavingAccountsData(List.of(simpleSavingAccount(1, "Test", 100, 500)));
        ExportException ex = assertThrows(ExportException.class,
                () -> exportService.exportSavingAccountsToXml("/neexistujuci/adresar/export.xml"));
        assertEquals("export.error.failed", ex.getMessageKey());
    }

    // ====================== HELPERS ======================

    private SavingAccountReportData simpleSavingAccount(int id, String name,
                                                         double savedUp, double target) {
        return SavingAccountReportData.builder()
                .accountId(id).accountName(name)
                .savedUp(savedUp).needToSave(target - savedUp).targetAmount(target)
                .targetDate(LocalDateTime.of(2027, 1, 1, 0, 0))
                .initialBalance(0).createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .progressGrouping("MONTH")
                .expectedProgress(Map.of()).actualProgress(Map.of())
                .transactions(List.of()).build();
    }

    private SavingAccountReportData savingAccountWithProgress(Map<String, Double> expected,
                                                               Map<String, Double> actual) {
        return SavingAccountReportData.builder()
                .accountId(1).accountName("Test")
                .savedUp(500).needToSave(500).targetAmount(1000)
                .targetDate(null).initialBalance(0).createdAt(null)
                .progressGrouping("MONTH")
                .expectedProgress(expected).actualProgress(actual)
                .transactions(List.of()).build();
    }

    private Document parseXml(Path path) throws Exception {
        return XmlUtil.createSecureFactory().newDocumentBuilder().parse(path.toFile());
    }

    // ====================== FAKE ======================

    static class FakeReportsService implements ReportsService {
        private List<SavingAccountReportData> savingAccountsData;
        private boolean throwOnSavingAccounts = false;

        void setSavingAccountsData(List<SavingAccountReportData> data) { this.savingAccountsData = data; }
        void setThrowOnSavingAccounts(boolean b) { this.throwOnSavingAccounts = b; }

        @Override
        public sk.sporixx.dto.WantNeedData loadWantNeedData(ChartPeriod period) { throw new UnsupportedOperationException(); }
        @Override
        public IncomeExpenseData loadIncomeExpenseData(ChartPeriod period) { throw new UnsupportedOperationException(); }
        @Override
        public sk.sporixx.dto.CategoryExpenseData loadCategoryExpenseData(ChartPeriod period) { throw new UnsupportedOperationException(); }
        @Override
        public sk.sporixx.dto.RecurringExpenseData loadRecurringExpenseData() { throw new UnsupportedOperationException(); }

        @Override
        public List<SavingAccountReportData> loadSavingAccountsData() {
            if (throwOnSavingAccounts) throw new RuntimeException("Simulated DB error");
            return savingAccountsData;
        }
    }
}

