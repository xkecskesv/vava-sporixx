package sk.sporixx.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import sk.sporixx.dto.IncomeExpenseData;
import sk.sporixx.dto.SavingAccountReportData;
import sk.sporixx.util.XmlUtil;

import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Implementácia ExportService.
 * Používa javax.xml DOM API pre generovanie XML.
 * Dáta načítava cez ReportsService (žiadny priamy prístup k DB).
 */
public class ExportServiceImpl implements ExportService {

    private static final Logger logger = LoggerFactory.getLogger(ExportServiceImpl.class);
    private static final DateTimeFormatter EXPORT_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ReportsService reportsService;

    public ExportServiceImpl(ReportsService reportsService) {
        this.reportsService = reportsService;
    }

    @Override
    public void exportIncomeExpenseToXml(int months, String filePath) {
        logger.info("Exporting income/expense report for {} months to {}", months, filePath);

        if (months <= 0) {
            throw new ExportException("export.error.invalid_months");
        }
        if (filePath == null || filePath.isBlank()) {
            throw new ExportException("export.error.invalid_path");
        }

        try {
            IncomeExpenseData data = reportsService.loadIncomeExpenseData(months);

            Document doc = createDocument();
            Element root = doc.createElement("IncomeExpenseReport");
            root.setAttribute("exportedAt", LocalDateTime.now().format(EXPORT_TIMESTAMP));
            root.setAttribute("months", String.valueOf(months));
            doc.appendChild(root);

            // Total sumy
            Element totals = doc.createElement("Totals");
            totals.setAttribute("totalIncome", String.valueOf(data.getTotalIncome()));
            totals.setAttribute("totalExpense", String.valueOf(data.getTotalExpense()));
            root.appendChild(totals);

            // Mesačné príjmy
            Element incomeEl = doc.createElement("MonthlyIncome");
            for (Map.Entry<String, Double> entry : data.getMonthlyIncome().entrySet()) {
                Element month = doc.createElement("Month");
                month.setAttribute("period", entry.getKey());
                month.setAttribute("amount", String.valueOf(entry.getValue()));
                incomeEl.appendChild(month);
            }
            root.appendChild(incomeEl);

            // Mesačné výdavky
            Element expenseEl = doc.createElement("MonthlyExpense");
            for (Map.Entry<String, Double> entry : data.getMonthlyExpense().entrySet()) {
                Element month = doc.createElement("Month");
                month.setAttribute("period", entry.getKey());
                month.setAttribute("amount", String.valueOf(entry.getValue()));
                expenseEl.appendChild(month);
            }
            root.appendChild(expenseEl);

            saveDocument(doc, filePath);
            logger.info("Income/expense report exported successfully to {}", filePath);

        } catch (ExportException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to export income/expense report", e);
            throw new ExportException("export.error.failed", e);
        }
    }

    @Override
    public void exportSavingAccountsToXml(String filePath) {
        logger.info("Exporting saving accounts report to {}", filePath);

        if (filePath == null || filePath.isBlank()) {
            throw new ExportException("export.error.invalid_path");
        }

        try {
            List<SavingAccountReportData> data = reportsService.loadSavingAccountsData();

            if (data.isEmpty()) {
                throw new ExportException("export.error.no_saving_accounts");
            }

            Document doc = createDocument();
            Element root = doc.createElement("SavingAccountsReport");
            root.setAttribute("exportedAt", LocalDateTime.now().format(EXPORT_TIMESTAMP));
            root.setAttribute("accountCount", String.valueOf(data.size()));
            doc.appendChild(root);

            for (SavingAccountReportData account : data) {
                Element accountEl = doc.createElement("SavingAccount");
                accountEl.setAttribute("id", String.valueOf(account.getAccountId()));
                accountEl.setAttribute("name", account.getAccountName());
                accountEl.setAttribute("savedUp", String.valueOf(account.getSavedUp()));
                accountEl.setAttribute("needToSave", String.valueOf(account.getNeedToSave()));
                accountEl.setAttribute("targetAmount", String.valueOf(account.getTargetAmount()));
                accountEl.setAttribute("progressGrouping", account.getProgressGrouping());

                // Expected progress
                Element expectedEl = doc.createElement("ExpectedProgress");
                for (Map.Entry<String, Double> entry : account.getExpectedProgress().entrySet()) {
                    Element point = doc.createElement("Point");
                    point.setAttribute("period", entry.getKey());
                    point.setAttribute("amount", String.valueOf(entry.getValue()));
                    expectedEl.appendChild(point);
                }
                accountEl.appendChild(expectedEl);

                // Actual progress
                Element actualEl = doc.createElement("ActualProgress");
                for (Map.Entry<String, Double> entry : account.getActualProgress().entrySet()) {
                    Element point = doc.createElement("Point");
                    point.setAttribute("period", entry.getKey());
                    point.setAttribute("amount", String.valueOf(entry.getValue()));
                    actualEl.appendChild(point);
                }
                accountEl.appendChild(actualEl);

                root.appendChild(accountEl);
            }

            saveDocument(doc, filePath);
            logger.info("Saving accounts report exported successfully to {}", filePath);

        } catch (ExportException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to export saving accounts report", e);
            throw new ExportException("export.error.failed", e);
        }
    }

    /**
     * Vytvorí prázdny DOM Document.
     * Bezpečnostné nastavenia zabraňujú XXE (XML External Entity) útokom:
     */
    private Document createDocument() throws Exception {
        return XmlUtil.createSecureFactory().newDocumentBuilder().newDocument();
    }

    /**
     * Uloží DOM Document do súboru.
     * Bezpečnostné nastavenia zabraňujú načítaniu externých DTD a stylesheetov.
     * Formátovanie: UTF-8, odsadenie 4 medzery pre čitateľnosť.
     */
    private void saveDocument(Document doc, String filePath) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(filePath));
        transformer.transform(source, result);
    }
}