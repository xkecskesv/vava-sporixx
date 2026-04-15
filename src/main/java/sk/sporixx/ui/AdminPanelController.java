package sk.sporixx.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import sk.sporixx.dto.AdminUserData;
import sk.sporixx.dto.CurrentUser;
import sk.sporixx.service.ProfileException;
import sk.sporixx.service.ServiceLocator;
import sk.sporixx.service.SessionManager;
import sk.sporixx.util.Localization;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * Controller for dedicated admin panel screen.
 */
public class AdminPanelController {

    @FXML private Label titleLabel;
    @FXML private Button logoutButton;
    @FXML private Label forcePasswordChangeLabel;

    @FXML private Label usersTitleLabel;
    @FXML private TableView<AdminUserData> usersTable;
    @FXML private TableColumn<AdminUserData, String> nameColumn;
    @FXML private TableColumn<AdminUserData, String> emailColumn;
    @FXML private TableColumn<AdminUserData, Boolean> familyManagerColumn;
    @FXML private TableColumn<AdminUserData, Boolean> activeColumn;

    @FXML private ImageView profileImage;
    @FXML private Button uploadPhotoButton;
    @FXML private Label firstNameLabel;
    @FXML private Label lastNameLabel;
    @FXML private Label emailLabel;
    @FXML private Label genderLabel;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private ComboBox<String> genderComboBox;
    @FXML private Button saveProfileButton;
    @FXML private Label profileFeedbackLabel;

    @FXML private Label oldPasswordLabel;
    @FXML private PasswordField oldPasswordField;
    @FXML private Label newPasswordLabel;
    @FXML private PasswordField newPasswordField;
    @FXML private Button changePasswordButton;
    @FXML private Label passwordFeedbackLabel;

    @FXML
    public void initialize() {
        if (!SessionManager.getInstance().isAdmin()) {
            redirectToLogin();
            return;
        }

        initTexts();
        initTable();
        if (!SessionManager.getInstance().isForcePasswordChange()) {
            loadUsers();
        }
        loadAdminProfile();
        applyForcedPasswordChangeState();
    }

    @FXML
    private void handleLogout() {
        try {
            ServiceLocator.getAuthService().logout();
            SceneManager.switchTo("login.fxml");
        } catch (Exception ignored) {
            // No-op in current UI flow.
        }
    }

    @FXML
    private void handleChangePassword() {
        String oldPassword = oldPasswordField.getText();
        String newPassword = newPasswordField.getText();

        try {
            ServiceLocator.getProfileService().changePassword(oldPassword, newPassword);
            oldPasswordField.clear();
            newPasswordField.clear();
            showPasswordFeedback(Localization.get("profile.management.password_changed"), true);
            if (SessionManager.getInstance().isForcePasswordChange()) {
                SessionManager.getInstance().setForcePasswordChange(false);
                loadUsers();
                applyForcedPasswordChangeState();
            }
        } catch (ProfileException e) {
            showPasswordFeedback(localizeMessage(e.getMessageKey()), false);
        } catch (Exception ignored) {
            showPasswordFeedback(Localization.get("profile.management.password_change_failed"), false);
        }
    }

    @FXML
    private void handleSaveProfile() {
        try {
            ServiceLocator.getProfileService().updateProfile(
                    firstNameField.getText(),
                    lastNameField.getText(),
                    emailField.getText(),
                    selectedGender());
            showProfileFeedback(Localization.get("profile.autosave.saved"), true);
            loadAdminProfile();
        } catch (ProfileException e) {
            showProfileFeedback(localizeMessage(e.getMessageKey()), false);
        } catch (Exception ignored) {
            showProfileFeedback(Localization.get("profile.autosave.failed"), false);
        }
    }

    @FXML
    private void handleUploadPhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Photo");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );

        File file = fileChooser.showOpenDialog(uploadPhotoButton.getScene().getWindow());
        if (file == null) {
            return;
        }

        try {
            profileImage.setImage(new Image(new FileInputStream(file), 170, 170, false, true));
            ServiceLocator.getProfileService().updateProfilePhoto(file.getAbsolutePath());
            showProfileFeedback(Localization.get("profile.autosave.saved"), true);
        } catch (ProfileException e) {
            showProfileFeedback(localizeMessage(e.getMessageKey()), false);
        } catch (Exception ignored) {
            showProfileFeedback(Localization.get("profile.autosave.failed"), false);
        }
    }

    private void initTexts() {
        titleLabel.setText(Localization.get("admin.title"));
        logoutButton.setText(Localization.get("profile.logout"));
        forcePasswordChangeLabel.setText(Localization.get("admin.password_change_required"));
        usersTitleLabel.setText(Localization.get("admin.users.title"));

        nameColumn.setText(Localization.get("admin.users.name"));
        emailColumn.setText(Localization.get("admin.users.email"));
        familyManagerColumn.setText(Localization.get("admin.users.family_manager"));
        activeColumn.setText(Localization.get("admin.users.active"));

        firstNameLabel.setText(Localization.get("profile.information.firstName"));
        lastNameLabel.setText(Localization.get("profile.information.lastName"));
        emailLabel.setText(Localization.get("profile.information.email"));
        genderLabel.setText(Localization.get("profile.information.gender"));
        uploadPhotoButton.setText(Localization.get("profile.management.photo"));
        saveProfileButton.setText(Localization.get("admin.profile.save"));

        genderComboBox.setItems(FXCollections.observableArrayList(
                Localization.get("profile.information.gender_male"),
                Localization.get("profile.information.gender_female"),
                "-"
        ));

        oldPasswordLabel.setText(Localization.get("profile.management.old_password"));
        newPasswordLabel.setText(Localization.get("profile.management.new_password"));
        changePasswordButton.setText(Localization.get("profile.management.change_password"));

        passwordFeedbackLabel.setVisible(false);
        passwordFeedbackLabel.setManaged(false);
        profileFeedbackLabel.setVisible(false);
        profileFeedbackLabel.setManaged(false);
    }

    private void initTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        familyManagerColumn.setCellValueFactory(new PropertyValueFactory<>("familyManager"));
        activeColumn.setCellValueFactory(new PropertyValueFactory<>("active"));
    }

    private void loadUsers() {
        List<AdminUserData> users = ServiceLocator.getAdminService().getAllUsers();
        usersTable.setItems(FXCollections.observableArrayList(users));
    }

    private void loadAdminProfile() {
        CurrentUser user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            redirectToLogin();
            return;
        }

        firstNameField.setText(safe(user.getName()));
        lastNameField.setText(safe(user.getSurname()));
        emailField.setText(safe(user.getEmail()));
        genderComboBox.setValue(ServiceLocator.getProfileService().toDisplayGender(user.getGender()));

        if (user.checkhasPhoto()) {
            try {
                profileImage.setImage(new Image(new FileInputStream(user.getPhotoPath())));
                return;
            } catch (FileNotFoundException ignored) {
                // Fallback below.
            }
        }
        profileImage.setImage(new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("/assets/icons/default_profile_picture.png"))));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String selectedGender() {
        String value = genderComboBox.getValue();
        return value == null ? "-" : value;
    }

    private void showPasswordFeedback(String message, boolean success) {
        passwordFeedbackLabel.setText(message);
        passwordFeedbackLabel.getStyleClass().removeAll("profile-feedback-success", "profile-feedback-error");
        passwordFeedbackLabel.getStyleClass().add(success ? "profile-feedback-success" : "profile-feedback-error");
        passwordFeedbackLabel.setVisible(true);
        passwordFeedbackLabel.setManaged(true);
    }

    private void showProfileFeedback(String message, boolean success) {
        profileFeedbackLabel.setText(message);
        profileFeedbackLabel.getStyleClass().removeAll("profile-feedback-success", "profile-feedback-error");
        profileFeedbackLabel.getStyleClass().add(success ? "profile-feedback-success" : "profile-feedback-error");
        profileFeedbackLabel.setVisible(true);
        profileFeedbackLabel.setManaged(true);
    }

    private void applyForcedPasswordChangeState() {
        boolean forced = SessionManager.getInstance().isForcePasswordChange();
        forcePasswordChangeLabel.setVisible(forced);
        forcePasswordChangeLabel.setManaged(forced);

        if (forced) {
            ObservableList<AdminUserData> empty = FXCollections.observableArrayList();
            usersTable.setItems(empty);
        }

        usersTable.setDisable(forced);
        uploadPhotoButton.setDisable(forced);
        firstNameField.setDisable(forced);
        lastNameField.setDisable(forced);
        emailField.setDisable(forced);
        genderComboBox.setDisable(forced);
        saveProfileButton.setDisable(forced);
    }

    private String localizeMessage(String key) {
        if (key == null || key.isBlank()) {
            return Localization.get("error.unexpected");
        }
        try {
            return Localization.get(key);
        } catch (Exception ignored) {
            return Localization.get("error.unexpected");
        }
    }

    private void redirectToLogin() {
        try {
            SceneManager.switchTo("login.fxml");
        } catch (Exception ignored) {
            // No-op in current UI flow.
        }
    }
}




