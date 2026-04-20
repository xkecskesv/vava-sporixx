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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.sporixx.dto.AdminUserData;
import sk.sporixx.dto.CurrentUser;
import sk.sporixx.model.User;
import sk.sporixx.service.ProfileException;
import sk.sporixx.service.ServiceLocator;
import sk.sporixx.service.SessionManager;
import sk.sporixx.util.Localization;

import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * Controller for dedicated admin panel screen.
 */
public class AdminPanelController {

    private static final Logger logger = LoggerFactory.getLogger(AdminPanelController.class);

    @FXML private Label titleLabel;
    @FXML private Button logoutButton;
    @FXML private Label forcePasswordChangeLabel;

    @FXML private Label usersTitleLabel;
    @FXML private TextField usersSearchField;
    @FXML private TableView<AdminUserData> usersTable;
    @FXML private TableColumn<AdminUserData, String> nameColumn;
    @FXML private TableColumn<AdminUserData, String> emailColumn;
    @FXML private TableColumn<AdminUserData, Boolean> familyManagerColumn;
    @FXML private TableColumn<AdminUserData, Boolean> activeColumn;

    @FXML private ImageView profileImage;
    @FXML private Button deactivateButton;
    @FXML private Button clearSelectionButton;
    @FXML private Label firstNameLabel;
    @FXML private Label lastNameLabel;
    @FXML private Label emailLabel;
    @FXML private Label genderLabel;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private ComboBox<String> genderComboBox;
    @FXML private Button cancelButton;
    @FXML private Button saveProfileButton;
    @FXML private Label profileFeedbackLabel;

    @FXML private Label oldPasswordLabel;
    @FXML private PasswordField oldPasswordField;
    @FXML private Label newPasswordLabel;
    @FXML private PasswordField newPasswordField;
    @FXML private Label confirmPasswordLabel;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label passwordFeedbackLabel;

    private ObservableList<AdminUserData> allUsers = FXCollections.observableArrayList();
    private AdminUserData selectedUser;

    @FXML
    public void initialize() {
        if (!SessionManager.getInstance().isAdmin()) {
            redirectToLogin();
            return;
        }

        initTexts();
        initTable();
        initSearch();
        initSelectionBinding();
        loadUsers();
        applyForcedPasswordChangeState();
    }

    @FXML
    private void handleLogout() {
        try {
            ServiceLocator.getAuthService().logout();
            SceneManager.switchTo("login.fxml");
        } catch (Exception e) {
            logger.error("Failed to logout from admin panel", e);
            showProfileFeedback(Localization.get("error.unexpected"), false);
        }
    }

    @FXML
    private void handleSaveProfile() {
        if (selectedUser == null) {
            showProfileFeedback(localizeMessage("admin.error.select_user"), false);
            return;
        }

        clearFeedback();
        boolean editingSelf = isEditingSelf(selectedUser);
        String oldPassword = oldPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (!validatePasswordInputs(editingSelf, oldPassword, newPassword, confirmPassword)) {
            return;
        }

        User editedUser = User.builder()
                .id(selectedUser.getId())
                .firstName(firstNameField.getText())
                .lastName(lastNameField.getText())
                .email(emailField.getText())
                .gender(selectedGender())
                .build();

        try {
            if (editingSelf) {
                ServiceLocator.getAdminService().changeOwnPassword(editedUser, oldPassword, newPassword);
                SessionManager.getInstance().setForcePasswordChange(false);
            } else {
                editedUser.setPasswordHash(newPassword);
            }

            ServiceLocator.getAdminService().updateUser(editedUser);
            showProfileFeedback(localizeMessage("admin.user.updated"), true);
            clearPasswordFields();
            refreshAndReselect(editedUser.getId());
            applyForcedPasswordChangeState();
        } catch (ProfileException e) {
            logger.warn("Failed to save selected user id={} from admin panel", selectedUser.getId(), e);
            showProfileFeedback(localizeMessage(e.getMessageKey()), false);
        } catch (Exception e) {
            logger.error("Unexpected error while saving selected user id={}", selectedUser.getId(), e);
            showProfileFeedback(Localization.get("error.unexpected"), false);
        }
    }

    @FXML
    private void handleDeactivateUser() {
        if (selectedUser == null) {
            showProfileFeedback(localizeMessage("admin.error.select_user"), false);
            return;
        }
        if (isEditingSelf(selectedUser)) {
            showProfileFeedback(localizeMessage("admin.error.cannot_deactivate_self"), false);
            return;
        }

        try {
            if (selectedUser.isActive()) {
                ServiceLocator.getAdminService().deactivateUser(User.builder().id(selectedUser.getId()).build());
                showProfileFeedback(localizeMessage("admin.user.deactivated"), true);
            } else {
                ServiceLocator.getAdminService().activateUser(User.builder().id(selectedUser.getId()).build());
                showProfileFeedback(localizeMessage("admin.user.activated"), true);
            }
            refreshAndReselect(selectedUser.getId());
        } catch (ProfileException e) {
            logger.warn("Failed to deactivate selected user id={} from admin panel", selectedUser.getId(), e);
            showProfileFeedback(localizeMessage(e.getMessageKey()), false);
        } catch (Exception e) {
            logger.error("Unexpected error while deactivating selected user id={}", selectedUser.getId(), e);
            showProfileFeedback(Localization.get("error.unexpected"), false);
        }
    }

    @FXML
    private void handleClearSelection() {
        usersTable.getSelectionModel().clearSelection();
        selectedUser = null;
        clearEditableForm();
        applySelectionState();
        clearFeedback();
    }

    @FXML
    private void handleCancelEdit() {
        clearFeedback();
        if (selectedUser == null) {
            clearEditableForm();
            return;
        }
        populateSelectedUser(selectedUser);
    }

    private void initTexts() {
        titleLabel.setText(Localization.get("admin.title"));
        logoutButton.setText(Localization.get("profile.logout"));
        forcePasswordChangeLabel.setText(Localization.get("admin.password_change_required"));
        usersTitleLabel.setText(Localization.get("admin.users.header"));
        usersSearchField.setPromptText(Localization.get("admin.users.search_placeholder"));

        nameColumn.setText(Localization.get("admin.users.name"));
        emailColumn.setText(Localization.get("admin.users.email"));
        familyManagerColumn.setText(Localization.get("admin.users.family_manager"));
        activeColumn.setText(Localization.get("admin.users.active"));

        firstNameLabel.setText(Localization.get("profile.information.firstName"));
        lastNameLabel.setText(Localization.get("profile.information.lastName"));
        emailLabel.setText(Localization.get("profile.information.email"));
        genderLabel.setText(Localization.get("profile.information.gender"));
        saveProfileButton.setText(Localization.get("admin.action.confirm"));
        deactivateButton.setText(Localization.get("admin.user.deactivate"));
        cancelButton.setText(Localization.get("admin.action.cancel"));

        genderComboBox.setItems(FXCollections.observableArrayList(
                Localization.get("profile.information.gender_male"),
                Localization.get("profile.information.gender_female"),
                "-"
        ));

        oldPasswordLabel.setText(Localization.get("profile.management.old_password"));
        newPasswordLabel.setText(Localization.get("profile.management.new_password"));
        confirmPasswordLabel.setText(Localization.get("register.password_confirm"));

        clearFeedback();
    }

    private void initTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        familyManagerColumn.setCellValueFactory(new PropertyValueFactory<>("familyManager"));
        activeColumn.setCellValueFactory(new PropertyValueFactory<>("active"));
    }

    private void initSearch() {
        usersSearchField.textProperty().addListener((observable, oldValue, newValue) -> applyUserFilter(newValue));
    }

    private void initSelectionBinding() {
        usersTable.getSelectionModel().selectedItemProperty().addListener((observable, oldUser, user) -> {
            clearFeedback();
            selectedUser = user;
            if (user == null) {
                clearEditableForm();
                applySelectionState();
                return;
            }
            populateSelectedUser(user);
        });
    }

    private void loadUsers() {
        List<AdminUserData> users = ServiceLocator.getAdminService().getAllUsers();
        allUsers = FXCollections.observableArrayList(users);
        applyUserFilter(usersSearchField.getText());
        if (!allUsers.isEmpty() && usersTable.getSelectionModel().getSelectedItem() == null) {
            usersTable.getSelectionModel().selectFirst();
        }
    }

    private void applyUserFilter(String query) {
        if (query == null || query.isBlank()) {
            usersTable.setItems(FXCollections.observableArrayList(allUsers));
            return;
        }

        String normalized = query.toLowerCase();
        ObservableList<AdminUserData> filtered = allUsers.filtered(user ->
                safe(user.getName()).toLowerCase().contains(normalized)
                        || safe(user.getEmail()).toLowerCase().contains(normalized)
        );
        usersTable.setItems(filtered);
    }

    private void populateSelectedUser(AdminUserData user) {
        if (user == null) {
            clearEditableForm();
            applySelectionState();
            return;
        }

        firstNameField.setText(safe(user.getFirstName()));
        lastNameField.setText(safe(user.getLastName()));
        emailField.setText(safe(user.getEmail()));
        genderComboBox.setValue(ServiceLocator.getProfileService().toDisplayGender(user.getGender()));
        clearPasswordFields();
        loadProfileImage(user.getPhotoPath());
        applySelectionState();
    }

    private void clearEditableForm() {
        firstNameField.clear();
        lastNameField.clear();
        emailField.clear();
        genderComboBox.setValue("-");
        clearPasswordFields();
        loadDefaultProfileImage();
    }

    private void clearPasswordFields() {
        oldPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
    }

    private void applySelectionState() {
        boolean hasSelection = selectedUser != null;
        boolean forced = SessionManager.getInstance().isForcePasswordChange();
        boolean editingSelf = hasSelection && isEditingSelf(selectedUser);

        firstNameField.setDisable(!hasSelection || forced);
        lastNameField.setDisable(!hasSelection || forced);
        emailField.setDisable(!hasSelection || forced);
        genderComboBox.setDisable(!hasSelection || forced);

        oldPasswordLabel.setVisible(hasSelection && editingSelf);
        oldPasswordLabel.setManaged(hasSelection && editingSelf);
        oldPasswordField.setVisible(hasSelection && editingSelf);
        oldPasswordField.setManaged(hasSelection && editingSelf);

        newPasswordLabel.setDisable(!hasSelection);
        newPasswordField.setDisable(!hasSelection);
        confirmPasswordLabel.setDisable(!hasSelection);
        confirmPasswordField.setDisable(!hasSelection);

        saveProfileButton.setDisable(!hasSelection);
        cancelButton.setDisable(!hasSelection || forced);
        deactivateButton.setDisable(!hasSelection || forced || editingSelf);
        clearSelectionButton.setDisable(!hasSelection || forced);

        if (!hasSelection) {
            deactivateButton.setText(Localization.get("admin.user.deactivate"));
        } else {
            deactivateButton.setText(Localization.get(selectedUser.isActive()
                    ? "admin.user.deactivate"
                    : "admin.user.activate"));
        }
    }

    private void refreshAndReselect(int userId) {
        loadUsers();
        AdminUserData refreshed = allUsers.stream()
                .filter(user -> user.getId() == userId)
                .findFirst()
                .orElse(null);

        if (refreshed != null) {
            usersTable.getSelectionModel().select(refreshed);
        } else {
            usersTable.getSelectionModel().clearSelection();
            selectedUser = null;
            clearEditableForm();
        }
        applySelectionState();
    }

    private boolean validatePasswordInputs(boolean editingSelf, String oldPassword, String newPassword, String confirmPassword) {
        if (editingSelf && (oldPassword == null || oldPassword.isBlank())) {
            showPasswordFeedback(localizeMessage("auth.error.old_password_required"), false);
            return false;
        }
        if (newPassword == null || newPassword.isBlank()) {
            showPasswordFeedback(localizeMessage("admin.error.password_required"), false);
            return false;
        }
        if (!Objects.equals(newPassword, confirmPassword)) {
            showPasswordFeedback(localizeMessage("admin.error.password_confirm_mismatch"), false);
            return false;
        }
        return true;
    }

    private void loadProfileImage(String photoPath) {
        if (photoPath == null || photoPath.isBlank()) {
            loadDefaultProfileImage();
            return;
        }

        File photoFile = new File(photoPath);
        if (!photoFile.exists() || !photoFile.isFile()) {
            loadDefaultProfileImage();
            return;
        }

        profileImage.setImage(new Image(photoFile.toURI().toString()));
    }

    private void loadDefaultProfileImage() {
        profileImage.setImage(new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("/assets/icons/default_profile_picture.png"))));
    }

    private boolean isEditingSelf(AdminUserData user) {
        CurrentUser current = SessionManager.getInstance().getCurrentUser();
        return current != null && user != null && current.getId() == user.getId();
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

    private void clearFeedback() {
        passwordFeedbackLabel.setVisible(false);
        passwordFeedbackLabel.setManaged(false);
        passwordFeedbackLabel.setText("");
        profileFeedbackLabel.setVisible(false);
        profileFeedbackLabel.setManaged(false);
        profileFeedbackLabel.setText("");
    }

    private void applyForcedPasswordChangeState() {
        boolean forced = SessionManager.getInstance().isForcePasswordChange();
        forcePasswordChangeLabel.setVisible(forced);
        forcePasswordChangeLabel.setManaged(forced);

        usersTable.setDisable(forced);
        usersSearchField.setDisable(forced);

        if (!forced) {
            applySelectionState();
            return;
        }

        CurrentUser currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            redirectToLogin();
            return;
        }

        usersTable.getSelectionModel().clearSelection();
        selectedUser = AdminUserData.builder()
                .id(currentUser.getId())
                .firstName(safe(currentUser.getName()))
                .lastName(safe(currentUser.getSurname()))
                .name(safe(currentUser.getFullName()))
                .email(safe(currentUser.getEmail()))
                .gender(safe(currentUser.getGender()))
                .photoPath(currentUser.getPhotoPath())
                .active(true)
                .familyManager(currentUser.checkisParent())
                .build();

        populateSelectedUser(selectedUser);
        cancelButton.setDisable(true);
        deactivateButton.setDisable(true);
        clearSelectionButton.setDisable(true);
    }

    private String localizeMessage(String key) {
        if (key == null || key.isBlank()) {
            return Localization.get("error.unexpected");
        }
        try {
            return Localization.get(key);
        } catch (Exception e) {
            logger.warn("Missing localization key={} in admin panel", key, e);
            return Localization.get("error.unexpected");
        }
    }

    private void redirectToLogin() {
        try {
            SceneManager.switchTo("login.fxml");
        } catch (Exception e) {
            logger.error("Failed to redirect to login from admin panel", e);
        }
    }
}



