package sk.sporixx.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.sporixx.model.User;
import sk.sporixx.repository.UserRepository;
import sk.sporixx.util.PasswordUtil;
import sk.sporixx.util.ValidationUtil;

import java.util.Optional;

/**
 * Default implementation for profile updates and password changes.
 */
public class ProfileServiceImpl implements ProfileService {

    private static final Logger logger = LoggerFactory.getLogger(ProfileServiceImpl.class);

    private final UserRepository userRepository;
    private final UserService userService;

    public ProfileServiceImpl(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Override
    public void updateProfile(String firstName, String lastName, String email, String gender) {
        User currentUser = requireLoggedUser();

        if (!ValidationUtil.isNotBlank(firstName)) {
            throw new ProfileException("auth.error.first_name_required");
        }
        if (!ValidationUtil.isNotBlank(lastName)) {
            throw new ProfileException("auth.error.last_name_required");
        }
        if (!ValidationUtil.isValidNamePart(firstName) || !ValidationUtil.isValidNamePartCharacters(firstName)) {
            throw new ProfileException("auth.error.invalid_first_name");
        }
        if (!ValidationUtil.isValidNamePart(lastName) || !ValidationUtil.isValidNamePartCharacters(lastName)) {
            throw new ProfileException("auth.error.invalid_last_name");
        }
        if (!ValidationUtil.isValidEmail(email)) {
            throw new ProfileException("auth.error.invalid_email");
        }

        String normalizedEmail = email.trim().toLowerCase();
        Optional<User> existingByEmail = userRepository.findByEmail(normalizedEmail);
        if (existingByEmail.isPresent() && existingByEmail.get().getId() != currentUser.getId()) {
            throw new ProfileException("auth.error.email_exists");
        }

        currentUser.setFirstName(ValidationUtil.normalizeName(firstName));
        currentUser.setLastName(ValidationUtil.normalizeName(lastName));
        currentUser.setEmail(normalizedEmail);
        currentUser.setGender(userService.normalizeGender(gender));

        try {
            userRepository.update(currentUser);
        } catch (Exception e) {
            logger.error("Failed to update profile for user id={}", currentUser.getId(), e);
            throw new ProfileException("error.unexpected", e);
        }
    }

    @Override
    public void changePassword(String oldPassword, String newPassword) {
        User currentUser = requireLoggedUser();

        if (!ValidationUtil.isNotBlank(oldPassword)) {
            throw new ProfileException("auth.error.old_password_required");
        }
        if (!PasswordUtil.verifyPassword(oldPassword, currentUser.getPasswordHash())) {
            throw new ProfileException("auth.error.wrong_old_password");
        }
        if (!ValidationUtil.isValidPassword(newPassword)) {
            throw new ProfileException("auth.error.password_too_short");
        }
        if (PasswordUtil.verifyPassword(newPassword, currentUser.getPasswordHash())) {
            throw new ProfileException("auth.error.same_password");
        }

        currentUser.setPasswordHash(PasswordUtil.hashPassword(newPassword));

        try {
            userRepository.update(currentUser);
        } catch (Exception e) {
            logger.error("Failed to change password for user id={}", currentUser.getId(), e);
            throw new ProfileException("error.unexpected", e);
        }
    }

    @Override
    public void updateProfilePhoto(String photoPath) {
        User currentUser = requireLoggedUser();

        if (!ValidationUtil.isNotBlank(photoPath)) {
            throw new ProfileException("error.unexpected");
        }

        currentUser.setPhotoPath(photoPath.trim());

        try {
            userRepository.update(currentUser);
        } catch (Exception e) {
            logger.error("Failed to update photo for user id={}", currentUser.getId(), e);
            throw new ProfileException("error.unexpected", e);
        }
    }

    @Override
    public String toDisplayGender(String rawGender) {
        return userService.toDisplayGender(rawGender);
    }

    private User requireLoggedUser() {
        User currentUser = SessionManager.getInstance().getCurrentUserInternal();
        if (currentUser == null) {
            throw new ProfileException("auth.error.invalid_credentials");
        }
        return currentUser;
    }
}

