package sk.sporixx.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.sporixx.model.GenderCode;
import sk.sporixx.util.PasswordUtil;
import sk.sporixx.util.ValidationUtil;
import sk.sporixx.dto.AdminUserData;
import sk.sporixx.model.Role;
import sk.sporixx.model.User;
import sk.sporixx.model.Account;
import sk.sporixx.repository.AccountRepository;
import sk.sporixx.repository.UserRepository;

import java.util.List;
import java.util.Optional;

/**
 * Default admin service implementation.
 */
public class AdminServiceImpl implements AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminServiceImpl.class);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final UserService userService;

    public AdminServiceImpl(UserRepository userRepository, AccountRepository accountRepository, UserService userService) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.userService = userService;
    }

    @Override
    public List<AdminUserData> getAllUsers() {
        requireAdmin();
        return userRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public User updateUser(User user) {
        requireAdmin();
        if (user == null || user.getId() <= 0) {
            throw new ProfileException("error.unexpected");
        }

        User existing = requireExistingUser(user.getId());
        validateIdentityFields(user.getFirstName(), user.getLastName(), user.getEmail());

        String normalizedEmail = user.getEmail().trim().toLowerCase();
        Optional<User> existingByEmail = userRepository.findByEmail(normalizedEmail);
        if (existingByEmail.isPresent() && existingByEmail.get().getId() != existing.getId()) {
            throw new ProfileException("auth.error.email_exists");
        }

        existing.setFirstName(ValidationUtil.normalizeName(user.getFirstName()));
        existing.setLastName(ValidationUtil.normalizeName(user.getLastName()));
        existing.setEmail(normalizedEmail);
        existing.setGender(userService.normalizeGender(user.getGender()));

        if (ValidationUtil.isNotBlank(user.getPasswordHash())) {
            if (!ValidationUtil.isValidPassword(user.getPasswordHash())) {
                throw new ProfileException("auth.error.password_too_short");
            }
            existing.setPasswordHash(PasswordUtil.hashPassword(user.getPasswordHash()));
        }

        return userRepository.update(existing);
    }

    @Override
    public User deactivateUser(User user) {
        requireAdmin();
        if (user == null || user.getId() <= 0) {
            throw new ProfileException("error.unexpected");
        }

        User currentUser = SessionManager.getInstance().getCurrentUserInternal();
        if (currentUser != null && currentUser.getId() == user.getId()) {
            throw new ProfileException("admin.error.cannot_deactivate_self");
        }

        User existing = requireExistingUser(user.getId());
        setUserAccountsActive(existing.getId(), false);
        return requireExistingUser(existing.getId());
    }

    @Override
    public User activateUser(User user) {
        requireAdmin();
        if (user == null || user.getId() <= 0) {
            throw new ProfileException("error.unexpected");
        }

        User existing = requireExistingUser(user.getId());
        setUserAccountsActive(existing.getId(), true);
        return requireExistingUser(existing.getId());
    }

    @Override
    public void deleteUser(User user) {
        requireAdmin();
        if (user == null || user.getId() <= 0) {
            throw new ProfileException("error.unexpected");
        }

        User currentUser = SessionManager.getInstance().getCurrentUserInternal();
        if (currentUser != null && currentUser.getId() == user.getId()) {
            throw new ProfileException("admin.error.cannot_delete_self");
        }

        requireExistingUser(user.getId());
        userRepository.deleteById(user.getId());
    }

    @Override
    public void changeOwnPassword(User user, String oldPassword, String newPassword) {
        requireAdmin();
        User currentUser = SessionManager.getInstance().getCurrentUserInternal();

        if (currentUser == null || user == null || currentUser.getId() != user.getId()) {
            throw new ProfileException("error.unexpected");
        }
        UserValidationSupport.validatePasswordChange(oldPassword, newPassword, currentUser);
        UserValidationSupport.applyPasswordChange(newPassword, currentUser);
        userRepository.update(currentUser);
    }

    private AdminUserData toDto(User user) {
        return AdminUserData.builder()
                .id(user.getId())
                .firstName(safe(user.getFirstName()))
                .lastName(safe(user.getLastName()))
                .name((safe(user.getFirstName()) + " " + safe(user.getLastName())).trim())
                .email(safe(user.getEmail()))
                .gender(user.getGender() == null ? GenderCode.UNKNOWN : user.getGender())
                .photoPath(user.getPhotoPath())
                .familyManager(user.getRole() == Role.FAMILY_MANAGER)
                .active(user.isActive())
                .build();
    }

    private void validateIdentityFields(String firstName, String lastName, String email) {
        UserValidationSupport.validateIdentity(firstName, lastName, email);
    }

    private User requireExistingUser(int userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ProfileException("error.unexpected"));
    }

    private void setUserAccountsActive(int userId, boolean active) {
        List<Account> accounts = accountRepository.findAllByOwnerUserId(userId);
        if (accounts.isEmpty()) {
            logger.warn("No accounts found for user id={} while changing active state to {}", userId, active);
            throw new ProfileException("admin.error.no_accounts");
        }

        for (Account account : accounts) {
            if (active) {
                accountRepository.activateById(account.getId());
            } else {
                accountRepository.deactivateById(account.getId());
            }
        }
    }

    private void requireAdmin() {
        if (!SessionManager.getInstance().isAdmin()) {
            throw new ProfileException("auth.error.invalid_credentials");
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

