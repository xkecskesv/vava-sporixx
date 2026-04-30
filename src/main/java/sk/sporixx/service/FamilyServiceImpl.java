package sk.sporixx.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.sporixx.dto.FamilyMemberData;
import sk.sporixx.dto.FamilyRequestData;
import sk.sporixx.model.*;
import sk.sporixx.repository.*;
import sk.sporixx.util.ValidationUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FamilyServiceImpl implements FamilyService {

    private static final Logger logger = LoggerFactory.getLogger(FamilyServiceImpl.class);

    private final AccountAccessRepository accountAccessRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final FamilyRequestRepository familyRequestRepository;
    private final SavingGoalRepository savingGoalRepository;

    public FamilyServiceImpl(AccountAccessRepository accountAccessRepository,
                             AccountRepository accountRepository,
                             UserRepository userRepository,
                             FamilyRequestRepository familyRequestRepository,
                             SavingGoalRepository savingGoalRepository) {
        this.accountAccessRepository = accountAccessRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.familyRequestRepository = familyRequestRepository;
        this.savingGoalRepository = savingGoalRepository;
    }

    @Override
    public List<FamilyMemberData> getFamilyMembers() {
        int managerId = SessionManager.getInstance().getCurrentUserId();
        logger.info("Loading family members for managerId={}", managerId);

        try {
            // načíta prístupy k cudzím účtom (nie k vlastným — self-referenčné záznamy pre rolu preskočíme)
            List<AccountAccess> accesses = accountAccessRepository.findByUserId(managerId);
            if (accesses.isEmpty()) return new ArrayList<>();

            // zoskupí account_id podľa owner_user_id
            Map<Integer, List<Account>> accountsByOwner = new java.util.LinkedHashMap<>();

            for (AccountAccess access : accesses) {
                Optional<Account> accountOpt = accountRepository.findById(access.getAccountId());
                if (accountOpt.isEmpty()) continue;

                Account account = accountOpt.get();
                int ownerId = account.getOwnerUserId();
                if (ownerId == managerId) continue; // preskočí vlastné účty

                accountsByOwner.computeIfAbsent(ownerId, k -> new ArrayList<>()).add(account);
            }

            // pre každého vlastníka načítaj profil
            List<FamilyMemberData> result = new ArrayList<>();
            for (Map.Entry<Integer, List<Account>> entry : accountsByOwner.entrySet()) {
                int ownerId = entry.getKey();
                Optional<User> userOpt = userRepository.findById(ownerId);
                if (userOpt.isEmpty()) continue;

                User user = userOpt.get();

                List<Account> childAccounts = entry.getValue();

                // nájde grantedAt
                LocalDateTime grantedAt = accesses.stream()
                        .filter(a -> childAccounts.stream()
                                .anyMatch(ca -> ca.getId() == a.getAccountId()))
                        .map(AccountAccess::getGrantedAt)
                        .findFirst()
                        .orElse(null);

                result.add(FamilyMemberData.builder()
                        .userId(user.getId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .email(user.getEmail())
                        .photoPath(user.getPhotoPath())
                        .grantedAt(grantedAt)
                        .accounts(entry.getValue())
                        .build());
            }

            logger.info("Loaded {} family members", result.size());
            return result;

        } catch (Exception e) {
            logger.error("Failed to load family members", e);
            throw new FamilyException("error.db_error", e);
        }
    }

    @Override
    public void sendFamilyRequest(String email) {
        logger.info("Sending family request to email={}", email);

        if (email == null || email.isBlank()) {
            throw new FamilyException("family.error.email_required");
        }

        Optional<User> userOpt = userRepository.findByEmail(email.toLowerCase());
        if (userOpt.isEmpty()) {
            throw new FamilyException("family.error.user_not_found");
        }

        User child = userOpt.get();

        if (child.getRole() != Role.USER) {
            throw new FamilyException("family.error.not_a_user");
        }

        int managerId = SessionManager.getInstance().getCurrentUserId();
        if (child.getId() == managerId) {
            throw new FamilyException("family.error.cannot_add_self");
        }

        List<Account> childAccounts = accountRepository.findByOwnerUserId(child.getId());
        if (childAccounts.isEmpty()) {
            throw new FamilyException("family.error.no_accounts");
        }

        // skontroluj, či už je member
        List<AccountAccess> existing = accountAccessRepository.findByUserId(managerId);
        boolean alreadyAdded = existing.stream()
                .anyMatch(a -> childAccounts.stream()
                        .anyMatch(ca -> ca.getId() == a.getAccountId()));
        if (alreadyAdded) {
            throw new FamilyException("family.error.already_member");
        }

        // skontroluje, či už existuje pending request
        if (familyRequestRepository.existsPending(managerId, child.getId())) {
            throw new FamilyException("family.error.request_already_sent");
        }

        try {
            familyRequestRepository.save(FamilyRequest.builder()
                    .fromUserId(managerId)
                    .toUserId(child.getId())
                    .status(FamilyRequest.STATUS_PENDING)
                    .createdAt(LocalDateTime.now())
                    .build());

            logger.info("Family request sent: from={}, to={}", managerId, child.getId());

        } catch (FamilyException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to send family request", e);
            throw new FamilyException("error.db_error", e);
        }
    }

    @Override
    public void removeFamilyMember(int userId) {
        logger.info("Removing family member userId={}", userId);

        int managerId = SessionManager.getInstance().getCurrentUserId();

        // skontroluje, že člen je v rodine
        List<AccountAccess> existing = accountAccessRepository.findByUserId(managerId);
        List<Account> childAccounts = accountRepository.findByOwnerUserId(userId);

        boolean isMember = existing.stream()
                .anyMatch(a -> childAccounts.stream()
                        .anyMatch(ca -> ca.getId() == a.getAccountId()));

        if (!isMember) {
            throw new FamilyException("family.error.not_a_member");
        }

        try {
            // zmaže všetky prístupy family managera k účtom tohto dieťaťa
            for (Account account : childAccounts) {
                accountAccessRepository.revokeAccess(managerId, account.getId());
            }

            logger.info("Family member removed: managerId={}, childId={}",
                    managerId, userId);

        } catch (FamilyException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to remove family member userId={}", userId, e);
            throw new FamilyException("error.db_error", e);
        }
    }

    @Override
    public void acceptFamilyRequest(int requestId) {
        FamilyRequest request = familyRequestRepository.findById(requestId)
                .orElseThrow(() -> new FamilyException("family.error.request_not_found"));

        // skontroluje, že request je pre prihláseného používateľa
        int currentUserId = SessionManager.getInstance().getCurrentUserId();
        if (request.getToUserId() != currentUserId) {
            throw new FamilyException("family.error.not_your_request");
        }

        // skontroluje max 2 rodičia
        List<Account> childAccounts = accountRepository
                .findByOwnerUserId(currentUserId);

        long parentCount = childAccounts.stream()
                .flatMap(ca -> accountAccessRepository
                        .findByAccountId(ca.getId()).stream())
                .filter(acc -> acc.getUserId() != currentUserId) // preskočí self-referenčné záznamy
                .map(AccountAccess::getUserId)
                .distinct()
                .count();

        if (parentCount >= 2) {
            throw new FamilyException("family.error.max_parents_reached");
        }

        // udelí prístup
        for (Account account : childAccounts) {
            accountAccessRepository.grantAccess(
                    request.getFromUserId(),
                    account.getId(),
                    Role.USER.getAccessLevel());
        }

        familyRequestRepository.updateStatus(requestId, FamilyRequest.STATUS_ACCEPTED);
        logger.info("Family request accepted: id={}", requestId);
    }

    @Override
    public void rejectFamilyRequest(int requestId) {
        FamilyRequest request = familyRequestRepository.findById(requestId)
                .orElseThrow(() -> new FamilyException("family.error.request_not_found"));

        int currentUserId = SessionManager.getInstance().getCurrentUserId();
        if (request.getToUserId() != currentUserId) {
            throw new FamilyException("family.error.not_your_request");
        }

        familyRequestRepository.updateStatus(requestId, FamilyRequest.STATUS_REJECTED);
        logger.info("Family request rejected: id={}", requestId);
    }

    @Override
    public List<FamilyRequestData> getPendingRequests() {
        int userId = SessionManager.getInstance().getCurrentUserId();
        logger.info("Loading pending requests for userId={}", userId);
        try {
            List<FamilyRequest> requests = familyRequestRepository
                    .findPendingByToUserId(userId);

            List<FamilyRequestData> result = new ArrayList<>();
            for (FamilyRequest request : requests) {
                Optional<User> userOpt = userRepository.findById(request.getFromUserId());
                if (userOpt.isEmpty()) continue;

                User parent = userOpt.get();
                result.add(FamilyRequestData.builder()
                        .requestId(request.getId())
                        .fromUserId(request.getFromUserId())
                        .fromFirstName(parent.getFirstName())
                        .fromLastName(parent.getLastName())
                        .fromEmail(parent.getEmail())
                        .createdAt(request.getCreatedAt())
                        .build());
            }
            return result;

        } catch (Exception e) {
            logger.error("Failed to load pending requests", e);
            throw new FamilyException("error.db_error", e);
        }
    }

    @Override
    public List<FamilyRequest> getSentRequests() {
        int managerId = SessionManager.getInstance().getCurrentUserId();
        logger.info("Loading sent requests for managerId={}", managerId);
        try {
            return familyRequestRepository.findPendingByFromUserId(managerId);
        } catch (Exception e) {
            logger.error("Failed to load sent requests", e);
            throw new FamilyException("error.db_error", e);
        }
    }

    @Override
    public void updateChildSavingAccount(int accountId, String description,
                                         double targetAmount, LocalDate targetDate) {
        logger.info("Family manager updating child saving account id={}", accountId);

        int managerId = SessionManager.getInstance().getCurrentUserId();

        // kontroluje prístup k cudziemu účtu (vlastné účty nepočítame)
        List<AccountAccess> accesses = accountAccessRepository.findByUserId(managerId);
        boolean hasAccess = accesses.stream()
                .filter(a -> {
                    Optional<Account> acc = accountRepository.findById(a.getAccountId());
                    return acc.isPresent() && acc.get().getOwnerUserId() != managerId;
                })
                .anyMatch(a -> a.getAccountId() == accountId);
        if (!hasAccess) {
            throw new FamilyException("family.error.no_access");
        }

        Optional<Account> accountOpt = accountRepository.findById(accountId);
        if (accountOpt.isEmpty()) {
            throw new FamilyException("family.error.account_not_found");
        }

        Account account = accountOpt.get();
        if (!account.isSavingAccount()) {
            throw new FamilyException("family.error.not_saving_account");
        }

        if (!ValidationUtil.isNotBlank(description)) {
            throw new FamilyException("family.error.description_required");
        }
        if (targetAmount <= 0) {
            throw new FamilyException("family.error.invalid_amount");
        }
        if (targetDate == null || targetDate.isBefore(LocalDate.now())) {
            throw new FamilyException("family.error.invalid_date");
        }
        if (targetAmount - account.getCurrentBalance() < 0.01) {
            throw new FamilyException("family.error.target_below_current");
        }

        try {
            account.setDescription(description);
            accountRepository.update(account);

            List<SavingGoal> goals = savingGoalRepository.findActiveByAccountId(accountId);
            if (!goals.isEmpty()) {
                SavingGoal goal = goals.getFirst();
                savingGoalRepository.updateTargetAmount(goal.getId(), targetAmount);
                savingGoalRepository.updateTargetDate(goal.getId(), targetDate.atStartOfDay());
            }

            logger.info("Child saving account updated: id={}", accountId);

        } catch (FamilyException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to update child saving account id={}", accountId, e);
            throw new FamilyException("error.db_error", e);
        }
    }
}