package sk.sporixx.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sk.sporixx.model.Account;
import sk.sporixx.model.AccountAccess;
import sk.sporixx.model.Role;
import sk.sporixx.model.SavingGoal;
import sk.sporixx.repository.AccountAccessRepository;
import sk.sporixx.repository.AccountRepository;
import sk.sporixx.repository.SavingGoalRepository;
import sk.sporixx.util.Localization;
import sk.sporixx.util.ValidationUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class AccountServiceImpl implements AccountService {

    private static final Logger logger = LoggerFactory.getLogger(AccountServiceImpl.class);

    private final AccountRepository accountRepository;
    private final SavingGoalRepository savingGoalRepository;
    private final AccountAccessRepository accountAccessRepository;

    public AccountServiceImpl(AccountRepository accountRepository, SavingGoalRepository savingGoalRepository, AccountAccessRepository accountAccessRepository) {
        this.accountRepository = accountRepository;
        this.savingGoalRepository = savingGoalRepository;
        this.accountAccessRepository = accountAccessRepository;
    }

    @Override
    public void createPrivateAccount(String description, double initialAmount) {
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
        grantAccessToParents(saved);

        logger.info("Private account created: id={}", saved.getId());
    }

    @Override
    public void createSavingAccount(String description, double initialAmount,
                                    double targetAmount, LocalDate targetDate) {

        validateAccountInput(description, initialAmount);

        // description sa použije ako goalName
        if (targetAmount <= 0) {
            throw new AccountException("account.error.goal_amount_invalid");
        }
        if (targetAmount > ValidationUtil.MAX_AMOUNT) {
            throw new AccountException("error.amount_too_large");
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
        grantAccessToParents(saved);

        logger.info("Saving account created: id={}, goal={}", saved.getId(), description);
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

        // zmaže account_access záznamy pre tento účet
        accountAccessRepository.revokeAllAccessForAccount(accountId);

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

    @Override
    public void updateSavingAccount(int accountId, String description,
                                    double targetAmount, LocalDate targetDate) {
        logger.info("Updating saving account id={}", accountId);

        if (!ValidationUtil.isNotBlank(description)) {
            throw new AccountException("account.error.description_required");
        }
        if (targetAmount <= 0) {
            throw new AccountException("account.error.goal_amount_invalid");
        }
        if (targetAmount > ValidationUtil.MAX_AMOUNT) {
            throw new AccountException("error.amount_too_large");
        }
        if (targetDate == null || targetDate.isBefore(LocalDate.now())) {
            throw new AccountException("account.error.goal_date_invalid");
        }

        Account account = SessionManager.getInstance().getAccountById(accountId);
        if (account == null) {
            throw new AccountException("account.error.not_found");
        }
        if (!account.isSavingAccount()) {
            throw new AccountException("account.error.not_saving_account");
        }

        // kontrola, že targetAmount > currentAmount
        if (targetAmount - account.getCurrentBalance() < 0.01) {
            throw new AccountException("account.error.target_below_initial");
        }

        try {
            // aktualizuje description účtu
            account.setDescription(description);
            accountRepository.update(account);

            // aktualizuje goal
            List<SavingGoal> goals = savingGoalRepository
                    .findActiveByAccountId(accountId);
            if (!goals.isEmpty()) {
                SavingGoal goal = goals.getFirst();
                savingGoalRepository.updateTargetAmount(goal.getId(), targetAmount);
                savingGoalRepository.updateTargetDate(goal.getId(),
                        targetDate.atStartOfDay());
            }

            logger.info("Saving account updated: id={}", accountId);

        } catch (AccountException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to update saving account id={}", accountId, e);
            throw new AccountException("error.db_error", e);
        }
    }

    // helper metódy
    private void validateAccountInput(String description, double initialAmount) {
        if (!ValidationUtil.isNotBlank(description)) {
            throw new AccountException("account.error.description_required");
        }
        if (initialAmount < 0) {
            throw new AccountException("account.error.negative_amount");
        }
        if (initialAmount > ValidationUtil.MAX_AMOUNT) {
            throw new AccountException("error.amount_too_large");
        }
    }

    private Account buildAccount(int accountTypeId, String description,
                                 double initialAmount) {

        // region berieme z main accountu
        Account mainAccount = SessionManager.getInstance().getAccounts().stream()
                .filter(a -> a.getAccountTypeId() == Account.MAIN_ACCOUNT)
                .findFirst()
                .orElseThrow(() -> new AccountException("account.error.no_main_account"));

        return Account.builder()
                .ownerUserId(SessionManager.getInstance().getCurrentUserId())
                .regionId(mainAccount.getRegionId())
                .accountTypeId(accountTypeId)
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
        if (targetAmount > ValidationUtil.MAX_AMOUNT) {
            throw new AccountException("error.amount_too_large");
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
                .description(description)
                .initialBalance(initialAmount)
                .currentBalance(initialAmount)
                .isActive(true)
                .createdAt(createdAt) // pôvodný dátum
                .build();

        Account savedAccount = accountRepository.save(savingAccount);
        SessionManager.getInstance().addAccount(savedAccount);
        grantAccessToParents(savedAccount);

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

    /**
     * Udelí prístup všetkým rodičom dieťaťa k novému účtu.
     * Rodičia sú tí, ktorí majú prístup k existujúcim účtom tohto používateľa.
     */
    private void grantAccessToParents(Account newAccount) {
        try {
            List<Account> ownerAccounts = accountRepository
                    .findByOwnerUserId(newAccount.getOwnerUserId());

            if (ownerAccounts.isEmpty()) return;

            // nájde rodičov cez existujúce účty
            Set<Integer> parentIds = ownerAccounts.stream()
                    .flatMap(a -> accountAccessRepository.findByAccountId(a.getId()).stream())
                    .map(AccountAccess::getUserId)
                    .collect(Collectors.toSet());

            for (int parentId : parentIds) {
                accountAccessRepository.grantAccess(
                        parentId, newAccount.getId(), Role.USER.getAccessLevel());
                logger.info("Granted access to parentId={} for new accountId={}",
                        parentId, newAccount.getId());
            }
        } catch (Exception e) {
            logger.warn("Failed to grant parent access for new account id={}",
                    newAccount.getId(), e);
        }
    }

    @Override
    public Optional<SavingGoal> getSavingGoal(int accountId) {
        logger.info("Loading saving goal for accountId={}", accountId);
        try {
            List<SavingGoal> goals = savingGoalRepository.findActiveByAccountId(accountId);
            return goals.isEmpty() ? Optional.empty() : Optional.of(goals.getFirst());
        } catch (Exception e) {
            logger.error("Failed to load saving goal for accountId={}", accountId, e);
            throw new AccountException("error.db_error", e);
        }
    }

    @Override
    public String getLocalizedDescription(Account account) {
        if (account.isMainAccount()) {
            return Localization.get("account.default.main_description");
        }
        if (account.isEmergencyFund()) {
            return Localization.get("account.default.emergency_description");
        }
        return account.getDescription();
    }
}
