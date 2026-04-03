package sk.sporixx.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import sk.sporixx.model.Account;
import sk.sporixx.model.SavingGoal;
import sk.sporixx.repository.SavingGoalRepository;
import sk.sporixx.util.XmlUtil;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Implementácia ImportService.
 * Parsuje XML súbory vygenerované ExportServiceImpl.
 * Aktualizuje saving goals podľa importovaných dát:
 * - currentAmount (savedUp)
 * - targetAmount
 * - targetDate
 */
public class ImportServiceImpl implements ImportService {

    private static final Logger logger = LoggerFactory.getLogger(ImportServiceImpl.class);
    private static final DateTimeFormatter EXPORT_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SavingGoalRepository savingGoalRepository;

    public ImportServiceImpl(SavingGoalRepository savingGoalRepository) {
        this.savingGoalRepository = savingGoalRepository;
    }

    @Override
    public void importSavingAccountsFromXml(String filePath) {
        logger.info("Importing saving accounts from XML: {}", filePath);

        if (filePath == null || filePath.isBlank()) {
            throw new ImportException("import.error.invalid_path");
        }

        try {
            Document doc = parseDocument(filePath);
            Element root = doc.getDocumentElement();

            if (!"SavingAccountsReport".equals(root.getTagName())) {
                throw new ImportException("import.error.invalid_xml_format");
            }

            List<Account> savingAccounts = SessionManager.getInstance().getAccounts()
                    .stream()
                    .filter(Account::isSavingAccount)
                    .toList();

            if (savingAccounts.isEmpty()) {
                throw new ImportException("import.error.no_saving_accounts");
            }

            NodeList accountNodes = root.getElementsByTagName("SavingAccount");
            int updatedCount = 0;

            for (int i = 0; i < accountNodes.getLength(); i++) {
                Element accountEl = (Element) accountNodes.item(i);
                String name = accountEl.getAttribute("name");
                double savedUp = Double.parseDouble(accountEl.getAttribute("savedUp"));
                double targetAmount = Double.parseDouble(accountEl.getAttribute("targetAmount"));
                String targetDateStr = accountEl.getAttribute("targetDate");

                // Nájdi zodpovedajúci saving účet podľa name
                Account matchingAccount = savingAccounts.stream()
                        .filter(a -> a.getDescription().equals(name))
                        .findFirst()
                        .orElse(null);

                if (matchingAccount == null) {
                    logger.warn("No matching saving account found for name: {}", name);
                    continue;
                }

                List<SavingGoal> goals = savingGoalRepository
                        .findActiveByAccountId(matchingAccount.getId());

                if (goals.isEmpty()) {
                    logger.warn("No active goal for account: {}", name);
                    continue;
                }

                SavingGoal goal = goals.getFirst();

                // Aktualizuj currentAmount
                savingGoalRepository.updateCurrentAmount(goal.getId(), savedUp);

                // Aktualizuj targetAmount
                savingGoalRepository.updateTargetAmount(goal.getId(), targetAmount);

                // Aktualizuj targetDate ak existuje
                if (!targetDateStr.isEmpty()) {
                    LocalDateTime targetDate = LocalDateTime.parse(
                            targetDateStr, EXPORT_TIMESTAMP);
                    savingGoalRepository.updateTargetDate(goal.getId(), targetDate);
                }

                // Aktualizuj SessionManager
                Account sessionAccount = SessionManager.getInstance()
                        .getAccountById(matchingAccount.getId());
                if (sessionAccount != null) {
                    sessionAccount.setCurrentBalance(savedUp);
                }

                logger.info("Updated saving goal '{}': savedUp={}, targetAmount={}, targetDate={}",
                        name, savedUp, targetAmount, targetDateStr);
                updatedCount++;
            }

            logger.info("Import completed — {} saving goals updated", updatedCount);

        } catch (ImportException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to import saving accounts XML", e);
            throw new ImportException("import.error.failed", e);
        }
    }

    /**
     * Parsuje XML súbor do DOM Document.
     * Bezpečnostné nastavenia zabraňujú XXE útokom.
     */
    private Document parseDocument(String filePath) throws Exception {
        return XmlUtil.createSecureFactory().newDocumentBuilder().parse(new File(filePath));
    }
}