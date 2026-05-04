package sk.sporixx.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import sk.sporixx.model.Account;
import sk.sporixx.model.SavingGoal;
import sk.sporixx.model.Transaction;
import sk.sporixx.repository.AccountRepository;
import sk.sporixx.repository.SavingGoalRepository;
import sk.sporixx.repository.TransactionRepository;
import sk.sporixx.util.ValidationUtil;
import sk.sporixx.util.XmlUtil;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementácia ImportService.
 * Parsuje XML súbory vygenerované ExportServiceImpl.
 */
public class ImportServiceImpl implements ImportService {

    private static final Logger logger = LoggerFactory.getLogger(ImportServiceImpl.class);
    private static final DateTimeFormatter EXPORT_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SavingGoalRepository savingGoalRepository;
    private final AccountService accountService;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public ImportServiceImpl(SavingGoalRepository savingGoalRepository, AccountService accountService,
                             TransactionRepository transactionRepository,
                             AccountRepository accountRepository) {
        this.savingGoalRepository = savingGoalRepository;
        this.accountService = accountService;
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
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

            NodeList accountNodes = root.getElementsByTagName("SavingAccount");
            int updatedCount = 0;
            int createdCount = 0;

            for (int i = 0; i < accountNodes.getLength(); i++) {
                Element accountEl = (Element) accountNodes.item(i);

                String name = accountEl.getAttribute("name");
                double savedUp = Double.parseDouble(accountEl.getAttribute("savedUp"));
                double targetAmount = Double.parseDouble(accountEl.getAttribute("targetAmount"));
                double initialBalance = parseDouble(accountEl.getAttribute("initialBalance"), savedUp);
                String targetDateStr = accountEl.getAttribute("targetDate");
                String createdAtStr = accountEl.getAttribute("createdAt");

                LocalDate targetDate = parseTargetDate(targetDateStr);
                LocalDateTime createdAt = parseDateTime(createdAtStr);

                Account matchingAccount = savingAccounts.stream()
                        .filter(a -> a.getDescription().equals(name))
                        .findFirst()
                        .orElse(null);

                int accountId;
                if (matchingAccount == null) {
                    Account newAccount = accountService.createSavingAccountFromImport(
                            name, initialBalance, targetAmount, targetDate, createdAt);
                    accountId = newAccount.getId();
                    logger.info("Created new saving account from import: {}", name);
                    createdCount++;
                } else {
                    updateExistingSavingAccount(
                            matchingAccount, savedUp, targetAmount, targetDateStr, targetDate);
                    accountId = matchingAccount.getId();
                    updatedCount++;
                }

                // Importuj transakcie
                NodeList txNodes = accountEl.getElementsByTagName("Transaction");
                if (txNodes.getLength() > 0) {
                    importTransactions(accountId, txNodes);
                }
            }

            logger.info("Import completed — {} updated, {} created", updatedCount, createdCount);

        } catch (ImportException e) {
            throw e;
        } catch (AccountException e) {
            throw new ImportException(e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Failed to import saving accounts XML", e);
            throw new ImportException("import.error.failed", e);
        }
    }

    // Helper - aktualizuje existujúci saving účet
    private void updateExistingSavingAccount(Account matchingAccount,
                                             double savedUp,
                                             double targetAmount,
                                             String targetDateStr,
                                             LocalDate targetDate) {
        List<SavingGoal> goals = savingGoalRepository
                .findActiveByAccountId(matchingAccount.getId());

        if (goals.isEmpty()) {
            logger.warn("No active goal for account: {}", matchingAccount.getDescription());
            return;
        }

        SavingGoal goal = goals.getFirst();

        if (savedUp > ValidationUtil.MAX_AMOUNT || savedUp < ValidationUtil.MIN_BALANCE) {
            throw new ImportException("error.balance_limit_exceeded");
        }
        if (targetAmount > ValidationUtil.MAX_AMOUNT || targetAmount <= 0) {
            throw new ImportException("error.amount_too_large");
        }

        savingGoalRepository.updateCurrentAmount(goal.getId(), savedUp);
        savingGoalRepository.updateTargetAmount(goal.getId(), targetAmount);

        if (!targetDateStr.isEmpty()) {
            savingGoalRepository.updateTargetDate(goal.getId(), targetDate.atStartOfDay());
        }
        // aktualizuje DB
        accountRepository.updateBalance(matchingAccount.getId(), savedUp);

        // aktualizuje SessionManager
        Account sessionAccount = SessionManager.getInstance()
                .getAccountById(matchingAccount.getId());
        if (sessionAccount != null) {
            sessionAccount.setCurrentBalance(savedUp);
        }

        logger.info("Updated saving account '{}': savedUp={}, targetAmount={}",
                matchingAccount.getDescription(), savedUp, targetAmount);
    }

    // Helper - parsuje targetDate
    private LocalDate parseTargetDate(String targetDateStr) {
        if (targetDateStr == null || targetDateStr.isEmpty()) {
            return LocalDate.now().plusYears(1);
        }
        try {
            return LocalDateTime.parse(targetDateStr, EXPORT_TIMESTAMP).toLocalDate();
        } catch (Exception e) {
            logger.warn("Could not parse targetDate '{}', using default", targetDateStr);
            return LocalDate.now().plusYears(1);
        }
    }

    // Helper - parsuje LocalDateTime
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(dateTimeStr, EXPORT_TIMESTAMP);
        } catch (Exception e) {
            logger.warn("Could not parse dateTime '{}', using now", dateTimeStr);
            return LocalDateTime.now();
        }
    }

    // Helper - parsuje double s fallback hodnotou
    private double parseDouble(String value, double fallback) {
        if (value == null || value.isEmpty()) return fallback;
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    /**
     * Parsuje XML súbor do DOM Document.
     * Bezpečnostné nastavenia zabraňujú XXE útokom.
     */
    private Document parseDocument(String filePath) throws Exception {
        return XmlUtil.createSecureFactory().newDocumentBuilder().parse(new File(filePath));
    }

    /**
     * Importuje transakcie pre saving účet.
     * Pri importe existujúceho účtu - vymaže staré TYPE_INCOME transakcie
     * a nahradí ich importovanými.
     * Pri importe nového účtu - len pridá transakcie.
     */
    private void importTransactions(int accountId, NodeList txNodes) {
        // vymaže existujúce TYPE_INCOME transakcie na účte
        List<Transaction> existing = transactionRepository
                .findByAccountIdAndDateRange(
                        accountId,
                        LocalDateTime.of(2000, 1, 1, 0, 0),
                        LocalDateTime.now())
                .stream()
                .filter(t -> t.getTransactionTypeId() == Transaction.TYPE_INCOME)
                .toList();

        for (Transaction tx : existing) {
            transactionRepository.deleteById(tx.getId());
        }

        // pridá importované transakcie
        for (int i = 0; i < txNodes.getLength(); i++) {
            Element txEl = (Element) txNodes.item(i);

            String dateStr = txEl.getAttribute("date");
            double amount = parseDouble(txEl.getAttribute("amount"), 0.0);
            String description = txEl.getAttribute("description");
            int categoryId = (int) parseDouble(txEl.getAttribute("categoryId"), 0);
            String currencyCode = txEl.getAttribute("currencyCode");

            if (amount <= 0 || amount > ValidationUtil.MAX_AMOUNT) continue;

            LocalDateTime date = parseDateTime(dateStr);

            Transaction tx = Transaction.builder()
                    .accountId(accountId)
                    .transactionTypeId(Transaction.TYPE_INCOME)
                    .spendingClassificationId(null)
                    .categoryId(categoryId)
                    .amount(amount)
                    .currencyCode(currencyCode.isEmpty() ? "EUR" : currencyCode)
                    .description(description)
                    .completeDate(date)
                    .createdAt(date)
                    .build();

            transactionRepository.save(tx);
            logger.info("Imported transaction: {} {} {}", description, amount, date);
        }
    }
}