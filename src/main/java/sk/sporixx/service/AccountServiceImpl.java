package sk.sporixx.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sk.sporixx.model.Account;
import sk.sporixx.model.SavingGoal;
import sk.sporixx.repository.AccountRepository;
import sk.sporixx.repository.SavingGoalRepository;
import sk.sporixx.util.ValidationUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AccountServiceImpl implements AccountService {

    private static final Logger logger = LoggerFactory.getLogger(AccountServiceImpl.class);

    private final AccountRepository accountRepository;
    private final SavingGoalRepository savingGoalRepository;

    public AccountServiceImpl(AccountRepository accountRepository, SavingGoalRepository savingGoalRepository) {
        this.accountRepository = accountRepository;
        this.savingGoalRepository = savingGoalRepository;
    }

    @Override
    public Account createPrivateAccount(String description, double initialAmount) {
        logger.info("Creating private account for userId={}", SessionManager.getInstance().getCurrentUserId());

        validateAccountInput(description, initialAmount);

        boolean duplicate = SessionManager.getInstance().getAccounts().stream()
                .anyMatch(a -> a.getDescription().equalsIgnoreCase(description.trim()));
        if (duplicate) {
            throw new AccountException("account.error.description_already_exists");
        }

        Account saved = accountRepository.save(buildAccount(
                Account.PRIVATE_ACCOUNT, description, initialAmount));

        SessionManager.getInstance().addAccount(saved);
        logger.info("Private account created: id={}", saved.getId());
        return saved;
    }

    @Override
    public Account createSavingAccount(String description, double initialAmount,
                                       double targetAmount, LocalDate targetDate) {

        validateAccountInput(description, initialAmount);

        // description sa použije ako goalName
        if (targetAmount <= 0) {
            throw new AccountException("account.error.goal_amount_invalid");
        }
        if (targetAmount - initialAmount < 0.01) {
            throw new AccountException("account.error.target_below_initial");
        }
        if (targetDate == null || targetDate.isBefore(LocalDate.now())) {
            throw new AccountException("account.error.goal_date_invalid");
        }

        boolean duplicate = SessionManager.getInstance().getAccounts().stream()
                .anyMatch(a -> a.getDescription().equalsIgnoreCase(description.trim()));
        if (duplicate) {
            throw new AccountException("account.error.description_already_exists");
        }

        Account saved = accountRepository.save(buildAccount(
                Account.SAVING_ACCOUNT, description, initialAmount));

        SavingGoal goal = SavingGoal.builder()
                .accountId(saved.getId())
                .name(description)        // description ako goalName
                .targetAmount(targetAmount)
                .currentAmount(initialAmount)
                .targetDate(targetDate.atStartOfDay())
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        savingGoalRepository.save(goal);
        SessionManager.getInstance().addAccount(saved);

        logger.info("Saving account created: id={}, goal={}", saved.getId(), description);
        return saved;
    }

    @Override
    public void deleteAccount(int accountId) {
        logger.info("Deleting account: id={}", accountId);

        Account account = SessionManager.getInstance().getAccountById(accountId);
        if (account == null) {
            throw new AccountException("account.error.not_found");
        }
        if (account.getAccountTypeId() == Account.MAIN_ACCOUNT ||
                account.getAccountTypeId() == Account.EMERGENCY_FUND) {
            throw new AccountException("account.error.cannot_delete_default");
        }

        accountRepository.deactivateById(accountId);
        SessionManager.getInstance().removeAccount(accountId);

        logger.info("Account deactivated: id={}", accountId);
    }

    @Override
    public void updateAccountDescription(int accountId, String description) {
        logger.info("Updating description for account: id={}", accountId);

        if (!ValidationUtil.isNotBlank(description)) {
            throw new AccountException("account.error.description_required");
        }

        Account account = SessionManager.getInstance().getAccountById(accountId);
        if (account == null) {
            throw new AccountException("account.error.not_found");
        }

        account.setDescription(description);
        accountRepository.update(account);

        logger.info("Account description updated: id={}", accountId);
    }

    // Helper metódy
    private void validateAccountInput(String description, double initialAmount) {
        if (!ValidationUtil.isNotBlank(description)) {
            throw new AccountException("account.error.description_required");
        }
        if (initialAmount < 0) {
            throw new AccountException("account.error.negative_amount");
        }
    }

    private Account buildAccount(int accountTypeId, String description,
                                 double initialAmount) {
        // Region a currency berieme z Main Accountu
        Account mainAccount = SessionManager.getInstance().getAccounts().stream()
                .filter(a -> a.getAccountTypeId() == Account.MAIN_ACCOUNT)
                .findFirst()
                .orElseThrow(() -> new AccountException("account.error.no_main_account"));

        return Account.builder()
                .ownerUserId(SessionManager.getInstance().getCurrentUserId())
                .regionId(mainAccount.getRegionId())
                .accountTypeId(accountTypeId)
                .defaultCurrencyCode(mainAccount.getDefaultCurrencyCode())
                .description(description)
                .initialBalance(initialAmount)
                .currentBalance(initialAmount)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Override
    public Account createSavingAccountFromImport(String description, double initialAmount,
                                                 double targetAmount, LocalDate targetDate,
                                                 LocalDateTime createdAt) {

        validateAccountInput(description, initialAmount);

        if (targetAmount <= 0) {
            throw new AccountException("account.error.goal_amount_invalid");
        }
        if (targetAmount - initialAmount < 0.01) {
            throw new AccountException("account.error.target_below_initial");
        }
        if (targetDate == null || targetDate.isBefore(LocalDate.now())) {
            throw new AccountException("account.error.goal_date_invalid");
        }

        Account mainAccount = SessionManager.getInstance().getAccounts().stream()
                .filter(a -> a.getAccountTypeId() == Account.MAIN_ACCOUNT)
                .findFirst()
                .orElseThrow(() -> new AccountException("account.error.no_main_account"));

        Account savingAccount = Account.builder()
                .ownerUserId(SessionManager.getInstance().getCurrentUserId())
                .regionId(mainAccount.getRegionId())
                .accountTypeId(Account.SAVING_ACCOUNT)
                .defaultCurrencyCode(mainAccount.getDefaultCurrencyCode())
                .description(description)
                .initialBalance(initialAmount)
                .currentBalance(initialAmount)
                .isActive(true)
                .createdAt(createdAt) // pôvodný dátum
                .build();

        Account savedAccount = accountRepository.save(savingAccount);
        SessionManager.getInstance().addAccount(savedAccount);

        SavingGoal goal = SavingGoal.builder()
                .accountId(savedAccount.getId())
                .name(description)
                .targetAmount(targetAmount)
                .currentAmount(initialAmount)
                .targetDate(targetDate.atStartOfDay())
                .isActive(true)
                .createdAt(createdAt) // pôvodný dátum
                .build();

        savingGoalRepository.save(goal);

        logger.info("Saving account created from import: {}, createdAt: {}",
                description, createdAt);
        return savedAccount;
    }
}
