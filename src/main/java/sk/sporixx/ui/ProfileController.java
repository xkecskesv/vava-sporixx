package sk.sporixx.ui;

import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.sporixx.dto.CurrentUser;
import sk.sporixx.service.ProfileException;
import sk.sporixx.service.ServiceLocator;
import sk.sporixx.service.SessionManager;
import sk.sporixx.util.Localization;
import sk.sporixx.util.ValidationUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.MissingResourceException;
import java.util.Objects;
import javafx.util.Duration;

/**
 * JavaFX controller for the Profile view.
 *
 * <p>Handles profile initialization, autosave for editable profile fields,
 * password updates, and profile photo upload/persistence integration.</p>
 *
 * @author Viktória Kecskés
 */
public class ProfileController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    private static final Duration AUTOSAVE_DELAY = Duration.millis(800);

    // Header section
    @FXML private Label profileTitle;
    @FXML private Label profileSubtitle;
    @FXML private Button logoutButton;

    // Account management section
    @FXML private Label managementTitle;
    @FXML private ImageView profileImage;
    @FXML private Button uploadPhotoButton;
    @FXML private Label oldPasswordLabel;
    @FXML private PasswordField oldPasswordField;
    @FXML private Label newPasswordLabel;
    @FXML private PasswordField newPasswordField;
    @FXML private Button changePasswordButton;
    @FXML private Label passwordFeedbackLabel;

    // Profile information section
    @FXML private Label firstNameLabel;
    @FXML private Label lastNameLabel;
    @FXML private Label emailLabel;
    @FXML private Label genderLabel;
    @FXML private Label autosaveFeedbackLabel;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private ComboBox<String> genderComboBox;
    @FXML private CheckBox parentCheckBox;
    @FXML private Label financialLevelLabel;
    @FXML private Label financialLevelValue;
    @FXML private ProgressBar xpProgressBar;

    // Milestones section
    @FXML private Label milestonesTitle;
    @FXML private Label milestone1Title;
    @FXML private Label milestone1Subtitle;
    @FXML private Label milestone1Desc;
    @FXML private Label milestone2Title;
    @FXML private Label milestone2Subtitle;
    @FXML private Label milestone2Desc;
    @FXML private Label milestone3Title;
    @FXML private Label milestone3Subtitle;
    @FXML private Label milestone3Desc;
    @FXML private Label milestone4Title;
    @FXML private Label milestone4Subtitle;
    @FXML private Label milestone4Desc;
    @FXML private Label milestonesFooter;

    private final PauseTransition autosaveTimer = new PauseTransition(AUTOSAVE_DELAY);
    private boolean autosaveEnabled;
    private String lastSavedFirstName = "";
    private String lastSavedLastName = "";
    private String lastSavedEmail = "";
    private String lastSavedGender = "-";

    @FXML
    public void initialize() {
        // Load localized labels for static UI content.
        profileTitle.setText(Localization.get("profile.title"));
        logoutButton.setText(Localization.get("profile.logout"));

        managementTitle.setText(Localization.get("profile.management.title"));
        uploadPhotoButton.setText(Localization.get("profile.management.photo"));
        oldPasswordLabel.setText(Localization.get("profile.management.old_password"));
        newPasswordLabel.setText(Localization.get("profile.management.new_password"));
        changePasswordButton.setText(Localization.get("profile.management.change_password"));
        passwordFeedbackLabel.setVisible(false);
        passwordFeedbackLabel.setManaged(false);
        oldPasswordField.setPromptText("***************");
        newPasswordField.setPromptText("***************");

        oldPasswordField.textProperty().addListener(obs -> {
            if (obs != null) hidePasswordFeedback();
        });
        newPasswordField.textProperty().addListener(obs -> {
            if (obs != null) hidePasswordFeedback();
        });

        profileSubtitle.setText(Localization.get("profile.information.title"));
        firstNameLabel.setText(Localization.get("profile.information.firstName"));
        lastNameLabel.setText(Localization.get("profile.information.lastName"));
        emailLabel.setText(Localization.get("profile.information.email"));
        genderLabel.setText(Localization.get("profile.information.gender"));
        autosaveFeedbackLabel.setVisible(false);
        autosaveFeedbackLabel.setManaged(false);
        parentCheckBox.setText("");
        financialLevelLabel.setText(Localization.get("profile.information.financial_level_title"));
        financialLevelValue.setText(Localization.get("profile.information.financial_level"));

        milestonesTitle.setText(Localization.get("profile.milestones.title"));
        milestone1Title.setText(Localization.get("profile.milestones.savings_master"));
        milestone1Subtitle.setText(Localization.get("profile.milestones.subtitle_getting_started"));
        milestone1Desc.setText(Localization.get("profile.milestones.savings_master_subtitle"));
        milestone2Title.setText(Localization.get("profile.milestones.budget_keeper"));
        milestone2Subtitle.setText(Localization.get("profile.milestones.subtitle_getting_started"));
        milestone2Desc.setText(Localization.get("profile.milestones.budget_keeper_subtitle"));
        milestone3Title.setText(Localization.get("profile.milestones.investor"));
        milestone3Subtitle.setText(Localization.get("profile.milestones.subtitle_getting_started"));
        milestone3Desc.setText(Localization.get("profile.milestones.investor_subtitle"));
        milestone4Title.setText(Localization.get("profile.milestones.smart_spender"));
        milestone4Subtitle.setText(Localization.get("profile.milestones.subtitle_getting_started"));
        milestone4Desc.setText(Localization.get("profile.milestones.smart_spender_subtitle"));
        milestonesFooter.setText(Localization.get("profile.milestones.footer"));

        genderComboBox.setItems(FXCollections.observableArrayList(
                Localization.get("profile.information.gender_male"),
                Localization.get("profile.information.gender_female"),
                "-"
        ));

        // TODO Temporary placeholder until XP data is provided by a dedicated service.
        xpProgressBar.setProgress(1.0);

        // Populate inputs from the active session, or show safe defaults.
        CurrentUser user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            firstNameField.setText("");
            lastNameField.setText("");
            emailField.setText("");
            genderComboBox.setValue("-");
            parentCheckBox.setSelected(false);
            loadFallbackAvatar();
            return;
        }

        firstNameField.setText(editableText(user.getName()));
        lastNameField.setText(editableText(user.getSurname()));
        emailField.setText(editableText(user.getEmail()));
        genderComboBox.setValue(ServiceLocator.getProfileService().toDisplayGender(user.getGender()));
        parentCheckBox.setSelected(user.checkisParent());

        loadUserPhoto(user);

        initAutosaveState();
        setupAutosaveHandlers();
    }

    /**
     * Logs out the current user and navigates back to the login view.
     */
    @FXML
    private void handleLogout() {
        try {
            ServiceLocator.getAuthService().logout();
            SceneManager.switchTo("login.fxml");
        } catch (Exception ignored) {
            // No-op in skeleton phase.
        }
    }

    /**
     * Opens file picker for profile photo, previews it immediately, and persists the photo path.
     */
    @FXML
    private void handleUploadPhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Photo");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );

        File file = fileChooser.showOpenDialog(uploadPhotoButton.getScene().getWindow());
        if (file == null) return;

        try {
            Image image = new Image(new FileInputStream(file), 210, 210, false, true);
            profileImage.setImage(image);

            Rectangle clip = new Rectangle(profileImage.getFitWidth(), profileImage.getFitHeight());
            clip.setArcWidth(30);
            clip.setArcHeight(30);
            profileImage.setClip(clip);

            ServiceLocator.getProfileService().updateProfilePhoto(file.getAbsolutePath());
        } catch (ProfileException ignored) {
            // Image preview stays visible; persistence is retried on next upload.
        } catch (FileNotFoundException e) {
            logger.warn("Selected profile image was not found: {}", file.getAbsolutePath(), e);
        }
    }

    /**
     * Handles password change request and shows inline one-line feedback.
     */
    @FXML
    private void handleChangePassword() {
        String oldPassword = oldPasswordField.getText();
        String newPassword = newPasswordField.getText();

        try {
            ServiceLocator.getProfileService().changePassword(oldPassword, newPassword);
            oldPasswordField.clear();
            newPasswordField.clear();
            showPasswordFeedback(Localization.get("profile.management.password_changed"), true);
        } catch (ProfileException e) {
            showPasswordFeedback(localizeMessage(e.getMessageKey()), false);
        } catch (Exception ignored) {
            showPasswordFeedback(Localization.get("profile.management.password_change_failed"), false);
        }
    }

    /**
     * Captures the last persisted profile values used by autosave diff checks.
     */
    private void initAutosaveState() {
        lastSavedFirstName = normalized(firstNameField.getText());
        lastSavedLastName = normalized(lastNameField.getText());
        lastSavedEmail = normalized(emailField.getText());
        lastSavedGender = selectedGender();
        autosaveEnabled = true;
    }

    /**
     * Registers debounced and focus-loss triggers for autosaving profile fields.
     */
    private void setupAutosaveHandlers() {
        autosaveTimer.setOnFinished(event -> {
            if (event != null) {
                flushAutosave();
            }
        });

        firstNameField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (obs != null && !Objects.equals(oldValue, newValue)) scheduleAutosave();
        });
        lastNameField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (obs != null && !Objects.equals(oldValue, newValue)) scheduleAutosave();
        });
        emailField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (obs != null && !Objects.equals(oldValue, newValue)) scheduleAutosave();
        });
        genderComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (obs != null && !Objects.equals(oldValue, newValue)) scheduleAutosave();
        });

        firstNameField.focusedProperty().addListener((obs, oldValue, isFocused) -> {
            if (obs != null && oldValue != isFocused && !isFocused) flushAutosave();
        });
        lastNameField.focusedProperty().addListener((obs, oldValue, isFocused) -> {
            if (obs != null && oldValue != isFocused && !isFocused) flushAutosave();
        });
        emailField.focusedProperty().addListener((obs, oldValue, isFocused) -> {
            if (obs != null && oldValue != isFocused && !isFocused) flushAutosave();
        });
        genderComboBox.focusedProperty().addListener((obs, oldValue, isFocused) -> {
            if (obs != null && oldValue != isFocused && !isFocused) flushAutosave();
        });
    }

    /**
     * Starts/restarts the autosave debounce timer.
     */
    private void scheduleAutosave() {
        if (!autosaveEnabled) return;
        showAutosaveFeedback(Localization.get("profile.autosave.saving"), "profile-feedback-pending");
        autosaveTimer.playFromStart();
    }

    /**
     * Persists profile fields when values changed and passed basic pre-validation.
     */
    private void flushAutosave() {
        if (!autosaveEnabled) return;

        String firstName = normalized(firstNameField.getText());
        String lastName = normalized(lastNameField.getText());
        String email = normalized(emailField.getText());
        String gender = selectedGender();

        boolean unchanged = firstName.equals(lastSavedFirstName)
                && lastName.equals(lastSavedLastName)
                && email.equals(lastSavedEmail)
                && gender.equals(lastSavedGender);
        if (unchanged) return;

        String validationMessageKey = validateAutosaveInput(firstName, lastName, email);
        if (validationMessageKey != null) {
            showAutosaveFeedback(localizeMessage(validationMessageKey), "profile-feedback-error");
            return;
        }

        try {
            showAutosaveFeedback(Localization.get("profile.autosave.saving"), "profile-feedback-pending");
            ServiceLocator.getProfileService().updateProfile(firstName, lastName, email, gender);
            lastSavedFirstName = firstName;
            lastSavedLastName = lastName;
            lastSavedEmail = email;
            lastSavedGender = gender;
            showAutosaveFeedback(Localization.get("profile.autosave.saved"), "profile-feedback-success");
        } catch (ProfileException e) {
            // Keep user input as-is; save is retried on next valid change.
            showAutosaveFeedback(localizeMessage(e.getMessageKey()), "profile-feedback-error");
        } catch (Exception e) {
            logger.warn("Autosave failed unexpectedly.", e);
            showAutosaveFeedback(Localization.get("profile.autosave.failed"), "profile-feedback-error");
        }
    }

    /**
     * Returns selected gender display value, or fallback dash when not selected.
     */
    private String selectedGender() {
        String value = genderComboBox.getValue();
        return value == null ? "-" : value;
    }

    /**
     * Trims input safely and converts null to empty string.
     */
    private String normalized(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * Resolves localized text by key with safe fallback for missing keys.
     */
    private String localizeMessage(String key) {
        if (key == null || key.isBlank()) {
            return Localization.get("error.unexpected");
        }
        try {
            return Localization.get(key);
        } catch (MissingResourceException ignored) {
            return Localization.get("error.unexpected");
        }
    }

    /**
     * Performs lightweight pre-validation for autosave and returns localization key when invalid.
     */
    private String validateAutosaveInput(String firstName, String lastName, String email) {
        if (!ValidationUtil.isNotBlank(firstName)) return "auth.error.first_name_required";
        if (!ValidationUtil.isNotBlank(lastName)) return "auth.error.last_name_required";
        if (!ValidationUtil.isValidEmail(email)) return "auth.error.invalid_email";
        if (!ValidationUtil.isValidNamePartCharacters(firstName) || !ValidationUtil.isValidNamePart(firstName)) {
            return "auth.error.invalid_first_name";
        }
        if (!ValidationUtil.isValidNamePartCharacters(lastName) || !ValidationUtil.isValidNamePart(lastName)) {
            return "auth.error.invalid_last_name";
        }
        return null;
    }

    /**
     * Shows one-line password feedback under the password action button.
     */
    private void showPasswordFeedback(String message, boolean success) {
        passwordFeedbackLabel.setText(message);
        passwordFeedbackLabel.getStyleClass().removeAll("profile-feedback-success", "profile-feedback-error");
        passwordFeedbackLabel.getStyleClass().add(success ? "profile-feedback-success" : "profile-feedback-error");
        passwordFeedbackLabel.setVisible(true);
        passwordFeedbackLabel.setManaged(true);
    }

    /**
     * Hides inline password feedback message.
     */
    private void hidePasswordFeedback() {
        passwordFeedbackLabel.setVisible(false);
        passwordFeedbackLabel.setManaged(false);
    }

    /**
     * Shows one-line autosave status below profile input fields.
     */
    private void showAutosaveFeedback(String message, String styleClass) {
        autosaveFeedbackLabel.setText(message);
        autosaveFeedbackLabel.getStyleClass().removeAll(
                "profile-feedback-success", "profile-feedback-error", "profile-feedback-pending");
        autosaveFeedbackLabel.getStyleClass().add(styleClass);
        autosaveFeedbackLabel.setVisible(true);
        autosaveFeedbackLabel.setManaged(true);
    }
    
    /**
     * Returns editable value for input controls without forcing visual placeholders as real data.
     */
    private String editableText(String value) {
        return (value == null || value.isBlank()) ? "" : value.trim();
    }

    /**
     * Loads user photo from persisted path; falls back to default avatar on failure.
     */
    private void loadUserPhoto(CurrentUser user) {
        if (user.checkhasPhoto()) {
            try {
                profileImage.setImage(new Image(new FileInputStream(user.getPhotoPath())));
                return;
            } catch (FileNotFoundException ignored) {
                // fallback below
            }
        }
        loadFallbackAvatar();
    }

    /**
     * Loads bundled default avatar image.
     */
    private void loadFallbackAvatar() {
        profileImage.setImage(new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("/assets/icons/default_profile_picture.png"))));
    }
}


