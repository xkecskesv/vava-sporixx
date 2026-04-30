package sk.sporixx.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.sporixx.model.Account;
import sk.sporixx.model.Role;
import sk.sporixx.model.User;
import sk.sporixx.repository.AccountAccessRepository;
import sk.sporixx.repository.AccountRepository;
import sk.sporixx.repository.FamilyRequestRepository;
import sk.sporixx.repository.UserRepository;
import sk.sporixx.util.ValidationUtil;

import java.util.List;
import java.util.Optional;

/**
 * Implementácia služby zodpovedná za operácie profilu aktuálne prihláseného používateľa.
 *
 * <p>Trieda validuje vstupy, vykonáva normalizáciu a ukladá zmeny cez
 * {@link UserRepository}.</p>
 *
 * @author Viktória Kecskés
 */
public class ProfileServiceImpl implements ProfileService {

    private static final Logger logger = LoggerFactory.getLogger(ProfileServiceImpl.class);

    private final UserRepository userRepository;
    private final UserService userService;
    private final AccountRepository accountRepository;
    private final AccountAccessRepository accountAccessRepository;
    private final FamilyRequestRepository familyRequestRepository;

    public ProfileServiceImpl(UserRepository userRepository, UserService userService,
                              AccountRepository accountRepository,
                              AccountAccessRepository accountAccessRepository,
                              FamilyRequestRepository familyRequestRepository) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.accountRepository = accountRepository;
        this.accountAccessRepository = accountAccessRepository;
        this.familyRequestRepository = familyRequestRepository;
    }

    /**
     * Aktualizuje identifikačné údaje profilu prihláseného používateľa.
     *
     * @param firstName nové meno
     * @param lastName nové priezvisko
     * @param email nová e-mailová adresa
     * @param gender surová hodnota pohlavia z UI
     * @param isParent či profil označuje používateľa ako rodinného manažéra
     * @throws ProfileException keď validácia zlyhá alebo sa zmena nepodarí uložiť
     */
    @Override
    public void updateProfile(String firstName, String lastName, String email, String gender, boolean isParent) {
        User currentUser = requireLoggedUser();

        UserValidationSupport.validateIdentity(firstName, lastName, email);

        String normalizedEmail = email.trim().toLowerCase();
        Optional<User> existingByEmail = userRepository.findByEmail(normalizedEmail);
        if (existingByEmail.isPresent() && existingByEmail.get().getId() != currentUser.getId()) {
            throw new ProfileException("auth.error.email_exists");
        }

        Role originalRole = currentUser.getRole();
        boolean roleMutable = originalRole != Role.ADMIN;

        if (roleMutable) {
            if (isParent && originalRole == Role.USER) {
                // Kontrola že nie je dieťaťom v rodinke (nezohľadňuje vlastné záznamy)
                List<Account> userAccounts = accountRepository.findByOwnerUserId(currentUser.getId());
                boolean isChild = userAccounts.stream()
                        .anyMatch(a -> accountAccessRepository.findByAccountId(a.getId())
                                .stream()
                                .anyMatch(acc -> acc.getUserId() != currentUser.getId()));
                if (isChild) {
                    throw new ProfileException("profile.error.already_child");
                }
            }

            if (!isParent && originalRole == Role.FAMILY_MANAGER) {
                // Kontrola že nemá deti — ignorujeme self-referenčné záznamy (vlastné účty)
                List<Account> ownAccounts = accountRepository.findByOwnerUserId(currentUser.getId());
                boolean hasChildren = accountAccessRepository
                        .findByUserId(currentUser.getId())
                        .stream()
                        .anyMatch(acc -> ownAccounts.stream()
                                .noneMatch(a -> a.getId() == acc.getAccountId()));
                if (hasChildren) {
                    throw new ProfileException("profile.error.has_children");
                }
            }
        }

        // Nastav hodnoty — rolu ešte nemeníme
        currentUser.setFirstName(ValidationUtil.normalizeName(firstName));
        currentUser.setLastName(ValidationUtil.normalizeName(lastName));
        currentUser.setEmail(normalizedEmail);
        currentUser.setGender(userService.normalizeGender(gender));

        Role newRole = roleMutable
                ? (isParent ? Role.FAMILY_MANAGER : Role.USER)
                : originalRole;

        try {
            currentUser.setRole(newRole);
            userRepository.update(currentUser);
            userRepository.updateFamilyManagerStatus(currentUser.getId(), isParent);
            if (!isParent && originalRole == Role.FAMILY_MANAGER) {
                familyRequestRepository.cancelAllPendingByFromUserId(currentUser.getId());
            }
            logger.info("Profile updated for user id={}", currentUser.getId());
        } catch (Exception e) {
            // Revert roly pri chybe
            currentUser.setRole(originalRole);
            logger.error("Failed to update profile for user id={}", currentUser.getId(), e);
            throw new ProfileException("error.unexpected", e);
        }
    }

    /**
     * Zmení heslo prihláseného používateľa.
     *
     * @param oldPassword aktuálne heslo v otvorenom texte na overenie
     * @param newPassword nové heslo v otvorenom texte, ktoré sa uloží ako hash
     * @throws ProfileException keď je pôvodné heslo neplatné, zlyhá kontrola pravidiel
     *                          alebo sa zmena nepodarí uložiť
     */
    @Override
    public void changePassword(String oldPassword, String newPassword) {
        User currentUser = requireLoggedUser();

        UserValidationSupport.validatePasswordChange(oldPassword, newPassword, currentUser);
        UserValidationSupport.applyPasswordChange(newPassword, currentUser);

        try {
            userRepository.update(currentUser);
        } catch (Exception e) {
            logger.error("Failed to change password for user id={}", currentUser.getId(), e);
            throw new ProfileException("error.unexpected", e);
        }
    }


    /**
     * Aktualizuje a uloží cestu k profilovej fotografii prihláseného používateľa.
     *
     * @param photoPath absolútna cesta k vybratej fotografii
     * @throws ProfileException keď je cesta prázdna alebo uloženie zlyhá
     */
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

    /**
     * Prevedie surovú hodnotu pohlavia na lokalizovaný text pre UI.
     *
     * @param rawGender surová uložená hodnota
     * @return lokalizovaný text pre UI alebo náhradná hodnota {@code "-"}
     */
    @Override
    public String toDisplayGender(String rawGender) {
        return userService.toDisplayGender(rawGender);
    }

    /**
     * Vráti aktuálne prihláseného používateľa zo session.
     *
     * @return aktuálny prihlásený {@link User}
     * @throws ProfileException keď v session nie je žiadny prihlásený používateľ
     */
    private User requireLoggedUser() {
        User currentUser = SessionManager.getInstance().getCurrentUserInternal();
        if (currentUser == null) {
            throw new ProfileException("auth.error.invalid_credentials");
        }
        return currentUser;
    }
}
