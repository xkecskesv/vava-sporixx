package sk.sporixx.ui;

import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
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
import sk.sporixx.dto.CurrentUser;
import sk.sporixx.service.ProfileException;
import sk.sporixx.service.ServiceLocator;
import sk.sporixx.service.SessionManager;
import sk.sporixx.util.Localization;
import sk.sporixx.util.ValidationUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Objects;
import javafx.util.Duration;

public class ProfileController {

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

    // Profile information section
    @FXML private Label firstNameLabel;
    @FXML private Label lastNameLabel;
    @FXML private Label emailLabel;
    @FXML private Label genderLabel;
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
        oldPasswordField.setPromptText("***************");
        newPasswordField.setPromptText("***************");

        profileSubtitle.setText(Localization.get("profile.information.title"));
        firstNameLabel.setText(Localization.get("profile.information.firstName"));
        lastNameLabel.setText(Localization.get("profile.information.lastName"));
        emailLabel.setText(Localization.get("profile.information.email"));
        genderLabel.setText(Localization.get("profile.information.gender"));
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

        // Temporary placeholder until XP data is provided by a dedicated service.
        xpProgressBar.setProgress(1.0);

        // Populate inputs from the active session, or show safe defaults.
        CurrentUser user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            firstNameField.setPromptText("-");
            lastNameField.setPromptText("-");
            emailField.setPromptText("-");
            genderComboBox.setValue("-");
            parentCheckBox.setSelected(false);
            loadFallbackAvatar();
            return;
        }

        firstNameField.setText(defaultText(user.getName()));
        lastNameField.setText(defaultText(user.getSurname()));
        emailField.setText(defaultText(user.getEmail()));
        genderComboBox.setValue(ServiceLocator.getProfileService().toDisplayGender(user.getGender()));
        parentCheckBox.setSelected(user.checkisParent());

        loadUserPhoto(user);

        initAutosaveState();
        setupAutosaveHandlers();
    }

    @FXML
    // Ends session and routes the user back to the login screen.
    private void handleLogout(ActionEvent event) {
        try {
            ServiceLocator.getAuthService().logout();
            SceneManager.switchTo("login.fxml");
        } catch (Exception ignored) {
            // No-op in skeleton phase.
        }
    }

    @FXML
    // Allows selecting a local image and applies rounded clipping for avatar display.
    private void handleUploadPhoto(ActionEvent event) {
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

            CurrentUser user = SessionManager.getInstance().getCurrentUser();
            if (user != null) {
                user.setPhotoPath(file.getAbsolutePath());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void initAutosaveState() {
        lastSavedFirstName = normalized(firstNameField.getText());
        lastSavedLastName = normalized(lastNameField.getText());
        lastSavedEmail = normalized(emailField.getText());
        lastSavedGender = selectedGender();
        autosaveEnabled = true;
    }

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

    private void scheduleAutosave() {
        if (!autosaveEnabled) return;
        autosaveTimer.playFromStart();
    }

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

        // Skip partial states during typing; validation in service still remains authoritative.
        if (!ValidationUtil.isNotBlank(firstName)
                || !ValidationUtil.isNotBlank(lastName)
                || !ValidationUtil.isValidEmail(email)) {
            return;
        }

        try {
            ServiceLocator.getProfileService().updateProfile(firstName, lastName, email, gender);
            lastSavedFirstName = firstName;
            lastSavedLastName = lastName;
            lastSavedEmail = email;
            lastSavedGender = gender;
        } catch (ProfileException ignored) {
            // Keep user input as-is; save is retried on next valid change.
        }
    }

    private String selectedGender() {
        String value = genderComboBox.getValue();
        return value == null ? "-" : value;
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim();
    }


    // Replaces null/blank values with a neutral placeholder for prompt rendering.
    private String defaultText(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }

    // Loads the user photo when available; otherwise falls back to the default avatar.
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

    // Loads application default profile image from bundled assets.
    private void loadFallbackAvatar() {
        profileImage.setImage(new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("/assets/icons/default_profile_picture.png"))));
    }
}


