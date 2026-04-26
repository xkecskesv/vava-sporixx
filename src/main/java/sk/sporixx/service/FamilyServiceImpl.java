package sk.sporixx.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.sporixx.dto.FamilyMemberData;
import sk.sporixx.model.*;
import sk.sporixx.repository.AccountAccessRepository;
import sk.sporixx.repository.AccountRepository;
import sk.sporixx.repository.FamilyRequestRepository;
import sk.sporixx.repository.UserRepository;

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

    public FamilyServiceImpl(AccountAccessRepository accountAccessRepository,
                             AccountRepository accountRepository,
                             UserRepository userRepository,
                             FamilyRequestRepository familyRequestRepository) {
        this.accountAccessRepository = accountAccessRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.familyRequestRepository = familyRequestRepository;
    }

    @Override
    public List<FamilyMemberData> getFamilyMembers() {
        int managerId = SessionManager.getInstance().getCurrentUserId();
        logger.info("Loading family members for managerId={}", managerId);

        try {
            // Načítaj všetky prístupy Family Managera
            List<AccountAccess> accesses = accountAccessRepository.findByUserId(managerId);
            if (accesses.isEmpty()) return new ArrayList<>();

            // Zoskup account_id podľa owner_user_id
            // Každý account_id → načítaj account → vezmi ownerUserId
            Map<Integer, List<Account>> accountsByOwner = new java.util.LinkedHashMap<>();

            for (AccountAccess access : accesses) {
                Optional<Account> accountOpt = accountRepository.findById(access.getAccountId());
                if (accountOpt.isEmpty()) continue;

                Account account = accountOpt.get();
                int ownerId = account.getOwnerUserId();

                accountsByOwner.computeIfAbsent(ownerId, k -> new ArrayList<>()).add(account);
            }

            // Pre každého vlastníka načítaj profil
            List<FamilyMemberData> result = new ArrayList<>();
            for (Map.Entry<Integer, List<Account>> entry : accountsByOwner.entrySet()) {
                int ownerId = entry.getKey();
                Optional<User> userOpt = userRepository.findById(ownerId);
                if (userOpt.isEmpty()) continue;

                User user = userOpt.get();

                List<Account> childAccounts = entry.getValue();

                // Nájdi grantedAt
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

        // Skontroluj či už je member
        List<AccountAccess> existing = accountAccessRepository.findByUserId(managerId);
        boolean alreadyAdded = existing.stream()
                .anyMatch(a -> childAccounts.stream()
                        .anyMatch(ca -> ca.getId() == a.getAccountId()));
        if (alreadyAdded) {
            throw new FamilyException("family.error.already_member");
        }

        // Skontroluj či už existuje pending request
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

        // Skontroluj že člen je v rodine
        List<AccountAccess> existing = accountAccessRepository.findByUserId(managerId);
        List<Account> childAccounts = accountRepository.findByOwnerUserId(userId);

        boolean isMember = existing.stream()
                .anyMatch(a -> childAccounts.stream()
                        .anyMatch(ca -> ca.getId() == a.getAccountId()));

        if (!isMember) {
            throw new FamilyException("family.error.not_a_member");
        }

        try {
            // Zmaž všetky prístupy Family Managera k účtom tohto dieťaťa
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

        // Skontroluj že request je pre prihláseného používateľa
        int currentUserId = SessionManager.getInstance().getCurrentUserId();
        if (request.getToUserId() != currentUserId) {
            throw new FamilyException("family.error.not_your_request");
        }

        // Skontroluj max 2 rodičia
        List<Account> childAccounts = accountRepository
                .findByOwnerUserId(currentUserId);

        long parentCount = childAccounts.stream()
                .flatMap(ca -> accountAccessRepository
                        .findByAccountId(ca.getId()).stream())
                .map(AccountAccess::getUserId)
                .distinct()
                .count();

        if (parentCount >= 2) {
            throw new FamilyException("family.error.max_parents_reached");
        }

        // Udeľ prístup
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
    public List<FamilyRequest> getPendingRequests() {
        int userId = SessionManager.getInstance().getCurrentUserId();
        logger.info("Loading pending requests for userId={}", userId);
        try {
            return familyRequestRepository.findPendingByToUserId(userId);
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
}