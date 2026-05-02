package sk.sporixx.service;

import org.junit.jupiter.api.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import sk.sporixx.dto.ChartPeriod;
import sk.sporixx.dto.IncomeExpenseData;
import sk.sporixx.dto.SavingAccountReportData;
import sk.sporixx.util.XmlUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testy pre ExportService.exportIncomeExpenseToXml().
 *
 * Stratégia:
 *  - ReportsService mockujeme cez fake vnútornú triedu
 *  - XML výstup parsujeme pomocou javax.xml DOM API a overujeme obsah
 *  - Každý test používa dočasný súbor, ktorý sa po teste zmaže
 */
@DisplayName("ExportService – exportIncomeExpenseToXml()")
class ExportServiceIncomeExpenseTest {

    private FakeReportsService fakeReports;
    private ExportService exportService;
    private Path tempFile;

    @BeforeEach
    void setUp() throws Exception {
        fakeReports = new FakeReportsService();
        exportService = new ExportServiceImpl(fakeReports);
        tempFile = Files.createTempFile("export-test-", ".xml");
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
                () -> exportService.exportIncomeExpenseToXml(ChartPeriod.ONE_MONTH, null));
        assertEquals("export.error.invalid_path", ex.getMessageKey());
    }

    @Test
    @DisplayName("prázdny filePath → ExportException s kľúčom invalid_path")
    void blankPath_throwsExportException() {
        ExportException ex = assertThrows(ExportException.class,
                () -> exportService.exportIncomeExpenseToXml(ChartPeriod.ONE_MONTH, "   "));
        assertEquals("export.error.invalid_path", ex.getMessageKey());
    }

    // ====================== ŠTRUKTÚRA XML ======================

    @Test
    @DisplayName("exportovaný súbor existuje a nie je prázdny")
    void exportCreatesFile() throws Exception {
        fakeReports.setIncomeExpenseData(simpleData(500, 300));
        exportService.exportIncomeExpenseToXml(ChartPeriod.ONE_MONTH, tempFile.toString());

        assertTrue(Files.exists(tempFile));
        assertTrue(Files.size(tempFile) > 0);
    }

    @Test
    @DisplayName("root element je IncomeExpenseReport")
    void rootElementName() throws Exception {
        fakeReports.setIncomeExpenseData(simpleData(500, 300));
        exportService.exportIncomeExpenseToXml(ChartPeriod.ONE_MONTH, tempFile.toString());

        Document doc = parseXml(tempFile);
        assertEquals("IncomeExpenseReport", doc.getDocumentElement().getTagName());
    }

    @Test
    @DisplayName("root obsahuje atribút period zodpovedajúci zvolenému ChartPeriod")
    void rootHasPeriodAttribute() throws Exception {
        fakeReports.setIncomeExpenseData(simpleData(500, 300));
        exportService.exportIncomeExpenseToXml(ChartPeriod.SIX_MONTHS, tempFile.toString());

        Document doc = parseXml(tempFile);
        assertEquals("SIX_MONTHS", doc.getDocumentElement().getAttribute("period"));
    }

    @Test
    @DisplayName("root obsahuje atribút exportedAt (nie je prázdny)")
    void rootHasExportedAt() throws Exception {
        fakeReports.setIncomeExpenseData(simpleData(500, 300));
        exportService.exportIncomeExpenseToXml(ChartPeriod.ONE_MONTH, tempFile.toString());

        Document doc = parseXml(tempFile);
        String exportedAt = doc.getDocumentElement().getAttribute("exportedAt");
        assertFalse(exportedAt.isBlank());
    }

    @Test
    @DisplayName("groupBy=DAY pre ONE_WEEK a ONE_MONTH")
    void groupByDay_forWeekAndMonth() throws Exception {
        fakeReports.setIncomeExpenseData(simpleData(100, 50));
        exportService.exportIncomeExpenseToXml(ChartPeriod.ONE_WEEK, tempFile.toString());

        Document doc = parseXml(tempFile);
        assertEquals("DAY", doc.getDocumentElement().getAttribute("groupBy"));
    }

    @Test
    @DisplayName("groupBy=MONTH pre SIX_MONTHS a TWELVE_MONTHS")
    void groupByMonth_forLongerPeriods() throws Exception {
        fakeReports.setIncomeExpenseData(simpleData(100, 50));
        exportService.exportIncomeExpenseToXml(ChartPeriod.TWELVE_MONTHS, tempFile.toString());

        Document doc = parseXml(tempFile);
        assertEquals("MONTH", doc.getDocumentElement().getAttribute("groupBy"));
    }

    // ====================== TOTALS ======================

    @Test
    @DisplayName("Totals element obsahuje správne totalIncome a totalExpense")
    void totalsElement_correctValues() throws Exception {
        fakeReports.setIncomeExpenseData(simpleData(1500.0, 800.0));
        exportService.exportIncomeExpenseToXml(ChartPeriod.ONE_MONTH, tempFile.toString());

        Document doc = parseXml(tempFile);
        Element totals = (Element) doc.getElementsByTagName("Totals").item(0);
        assertNotNull(totals);
        assertEquals("1500.0", totals.getAttribute("totalIncome"));
        assertEquals("800.0", totals.getAttribute("totalExpense"));
    }

    @Test
    @DisplayName("Totals s nulovými hodnotami sa exportuje správne")
    void totalsElement_zeroValues() throws Exception {
        fakeReports.setIncomeExpenseData(simpleData(0, 0));
        exportService.exportIncomeExpenseToXml(ChartPeriod.ONE_MONTH, tempFile.toString());

        Document doc = parseXml(tempFile);
        Element totals = (Element) doc.getElementsByTagName("Totals").item(0);
        assertEquals("0.0", totals.getAttribute("totalIncome"));
        assertEquals("0.0", totals.getAttribute("totalExpense"));
    }

    // ====================== PERIOD ENTRIES ======================

    @Test
    @DisplayName("PeriodIncome obsahuje správny počet Entry elementov")
    void periodIncome_correctEntryCount() throws Exception {
        Map<String, Double> income = new LinkedHashMap<>();
        income.put("2026-01", 500.0);
        income.put("2026-02", 600.0);
        income.put("2026-03", 700.0);
        fakeReports.setIncomeExpenseData(IncomeExpenseData.builder()
                .monthlyIncome(income)
                .monthlyExpense(Map.of())
                .totalIncome(1800).totalExpense(0).build());

        exportService.exportIncomeExpenseToXml(ChartPeriod.SIX_MONTHS, tempFile.toString());

        Document doc = parseXml(tempFile);
        Element periodIncome = (Element) doc.getElementsByTagName("PeriodIncome").item(0);
        NodeList entries = periodIncome.getElementsByTagName("Entry");
        assertEquals(3, entries.getLength());
    }

    @Test
    @DisplayName("PeriodExpense Entry má správne atribúty period a amount")
    void periodExpense_entryAttributes() throws Exception {
        Map<String, Double> expense = new LinkedHashMap<>();
        expense.put("2026-03", 450.0);
        fakeReports.setIncomeExpenseData(IncomeExpenseData.builder()
                .monthlyIncome(Map.of())
                .monthlyExpense(expense)
                .totalIncome(0).totalExpense(450).build());

        exportService.exportIncomeExpenseToXml(ChartPeriod.ONE_MONTH, tempFile.toString());

        Document doc = parseXml(tempFile);
        Element periodExpense = (Element) doc.getElementsByTagName("PeriodExpense").item(0);
        Element entry = (Element) periodExpense.getElementsByTagName("Entry").item(0);
        assertEquals("2026-03", entry.getAttribute("period"));
        assertEquals("450.0", entry.getAttribute("amount"));
    }

    @Test
    @DisplayName("Prázdne mapy – PeriodIncome a PeriodExpense existujú ale nemajú Entry")
    void emptyMaps_noEntries() throws Exception {
        fakeReports.setIncomeExpenseData(IncomeExpenseData.builder()
                .monthlyIncome(Map.of())
                .monthlyExpense(Map.of())
                .totalIncome(0).totalExpense(0).build());

        exportService.exportIncomeExpenseToXml(ChartPeriod.ONE_MONTH, tempFile.toString());

        Document doc = parseXml(tempFile);
        Element periodIncome = (Element) doc.getElementsByTagName("PeriodIncome").item(0);
        Element periodExpense = (Element) doc.getElementsByTagName("PeriodExpense").item(0);
        assertNotNull(periodIncome);
        assertNotNull(periodExpense);
        assertEquals(0, periodIncome.getElementsByTagName("Entry").getLength());
        assertEquals(0, periodExpense.getElementsByTagName("Entry").getLength());
    }

    // ====================== CHYBOVÉ SCENÁRE ======================

    @Test
    @DisplayName("ReportsService hodí výnimku → ExportException s kľúčom failed")
    void reportsServiceException_throwsExportException() {
        fakeReports.setThrowOnIncomeExpense(true);

        ExportException ex = assertThrows(ExportException.class,
                () -> exportService.exportIncomeExpenseToXml(ChartPeriod.ONE_MONTH,
                        tempFile.toString()));
        assertEquals("export.error.failed", ex.getMessageKey());
    }

    @Test
    @DisplayName("neplatná cesta k súboru → ExportException s kľúčom failed")
    void invalidDirectory_throwsExportException() {
        fakeReports.setIncomeExpenseData(simpleData(100, 50));

        ExportException ex = assertThrows(ExportException.class,
                () -> exportService.exportIncomeExpenseToXml(ChartPeriod.ONE_MONTH,
                        "/neexistujuci/adresar/export.xml"));
        assertEquals("export.error.failed", ex.getMessageKey());
    }

    // ====================== HELPERS ======================

    private IncomeExpenseData simpleData(double income, double expense) {
        return IncomeExpenseData.builder()
                .monthlyIncome(Map.of("2026-03", income))
                .monthlyExpense(Map.of("2026-03", expense))
                .totalIncome(income)
                .totalExpense(expense)
                .build();
    }

    private Document parseXml(Path path) throws Exception {
        return XmlUtil.createSecureFactory()
                .newDocumentBuilder()
                .parse(path.toFile());
    }

    // ====================== FAKE ======================

    static class FakeReportsService implements ReportsService {
        private IncomeExpenseData incomeExpenseData;
        private boolean throwOnIncomeExpense = false;

        void setIncomeExpenseData(IncomeExpenseData data) { this.incomeExpenseData = data; }
        void setThrowOnIncomeExpense(boolean b) { this.throwOnIncomeExpense = b; }

        @Override
        public sk.sporixx.dto.WantNeedData loadWantNeedData(ChartPeriod period) {
            throw new UnsupportedOperationException();
        }

        @Override
        public IncomeExpenseData loadIncomeExpenseData(ChartPeriod period) {
            if (throwOnIncomeExpense) throw new RuntimeException("Simulated DB error");
            return incomeExpenseData;
        }

        @Override
        public sk.sporixx.dto.CategoryExpenseData loadCategoryExpenseData(ChartPeriod period) {
            throw new UnsupportedOperationException();
        }

        @Override
        public sk.sporixx.dto.RecurringExpenseData loadRecurringExpenseData() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<SavingAccountReportData> loadSavingAccountsData() {
            throw new UnsupportedOperationException();
        }
    }
}



