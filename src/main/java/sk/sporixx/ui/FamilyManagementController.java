package sk.sporixx.ui;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import sk.sporixx.dto.FamilyMemberData;
import sk.sporixx.model.Account;
import sk.sporixx.model.FamilyRequest;
import sk.sporixx.model.SavingGoal;
import sk.sporixx.service.ServiceLocator;
import sk.sporixx.util.AvatarUtil;
import sk.sporixx.util.Localization;

import java.io.FileInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class FamilyManagementController {

    private static final int MAX_VISIBLE_MEMBERS = 3;
    private static final int MAX_VISIBLE_ACCOUNT_CARDS = 2;

    // ── Family Members ───────────────────────────────────────────
    @FXML private HBox membersRow;
    @FXML private Button addUserBtn;
    @FXML private Label familyErrorLabel;

    // ── Family Accounts ──────────────────────────────────────────
    @FXML private VBox familyAccountsSection;
    @FXML private Label familyAccountsTitle;
    @FXML private HBox familyDefaultAccountsRow;
    @FXML private HBox familySavingAccountsRow;
    @FXML private Label familyAccountsErrorLabel;

    // ── Add User Modal ───────────────────────────────────────────
    @FXML private StackPane addUserModalOverlay;
    @FXML private TextField addUserEmailField;
    @FXML private Label addUserModalErrorLabel;

    // ── Edit Saving Modal ────────────────────────────────────────
    @FXML private StackPane editSavingModalOverlay;
    @FXML private TextField editSavingDescField;
    @FXML private TextField editSavingGoalField;
    @FXML private DatePicker editSavingDatePicker;
    @FXML private Label editSavingModalErrorLabel;

    // ── State ────────────────────────────────────────────────────
    private List<FamilyMemberData> allMembers = new ArrayList<>();
    private int membersOffset = 0;
    private FamilyMemberData selectedMember = null;

    private List<Account> selectedMemberDefaultAccounts = new ArrayList<>();
    private List<Account> selectedMemberSavingAccounts = new ArrayList<>();
    private int defaultAccountsOffset = 0;
    private int savingAccountsOffset = 0;

    private Account editingAccount = null;

    // ════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        loadFamilyMembers();
    }

    // ════════════════════════════════════════════════════════════
    //  FAMILY MEMBERS
    // ════════════════════════════════════════════════════════════
    private void loadFamilyMembers() {
        try {
            allMembers = ServiceLocator.getFamilyService().getFamilyMembers();
        } catch (Exception e) {
            allMembers = List.of();
            showFamilyError("error.db_error");
        }
        membersOffset = 0;
        renderMembers();
    }

    private void renderMembers() {
        membersRow.getChildren().clear();

        if (allMembers.isEmpty()) {
            Label empty = new Label(Localization.get("family.members.empty"));
            empty.getStyleClass().add("analytics-subtitle");
            membersRow.getChildren().add(empty);
            return;
        }

        // Ľavá šípka
        boolean canLeft = membersOffset > 0;
        membersRow.getChildren().add(createArrow(true, canLeft, () -> {
            membersOffset = Math.max(0, membersOffset - 1);
            renderMembers();
        }));

        int from = membersOffset;
        int to = Math.min(from + MAX_VISIBLE_MEMBERS, allMembers.size());
        for (int i = from; i < to; i++) {
            FamilyMemberData member = allMembers.get(i);
            boolean isSelected = selectedMember != null && selectedMember.getUserId() == member.getUserId();
            membersRow.getChildren().add(createMemberCard(member, isSelected));
        }

        boolean canRight = to < allMembers.size();
        membersRow.getChildren().add(createArrow(false, canRight, () -> {
            membersOffset++;
            renderMembers();
        }));
    }

    private VBox createMemberCard(FamilyMemberData member, boolean isSelected) {
        VBox card = new VBox(12);
        card.getStyleClass().add(isSelected ? "account-card-active" : "account-card");
        card.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(card, Priority.ALWAYS);

        // Header: meno + delete button
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox nameBox = new VBox(2);
        Label name = new Label(member.getFirstName() + " " + member.getLastName());
        name.getStyleClass().add(isSelected ? "account-card-title-active" : "account-card-title");
        Label email = new Label(member.getEmail());
        email.getStyleClass().add(isSelected ? "account-card-desc-active" : "analytics-subtitle");
        nameBox.getChildren().addAll(name, email);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        header.getChildren().add(nameBox);

        Button deleteBtn = new Button();
        deleteBtn.getStyleClass().add("btn-icon-danger");
        try {
            ImageView icon = new ImageView(new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/assets/icons/icon_delete.png"))));
            icon.setFitWidth(14); icon.setFitHeight(14); icon.setPreserveRatio(true);
            deleteBtn.setGraphic(icon);
        } catch (Exception ignored) {}
        deleteBtn.setOnAction(e -> handleRemoveMember(member));
        header.getChildren().add(deleteBtn);

        // Fotka
        ImageView photo = new ImageView();
        photo.setFitWidth(72); photo.setFitHeight(72); photo.setPreserveRatio(true);
        Circle clip = new Circle(36, 36, 36);
        photo.setClip(clip);
        AvatarUtil.apply(photo, member.getPhotoPath(), 72, getClass());

        // Info riadok
        Label addedLabel = new Label(Localization.get("family.member.added") + ": "
                + (member.getGrantedAt() != null
                ? member.getGrantedAt().toLocalDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                : "-"));
        addedLabel.getStyleClass().add(isSelected ? "account-card-desc-active" : "analytics-subtitle");

        // Bankové ikonky — každá ikona = 1 účet
        HBox accountIcons = new HBox(8);
        accountIcons.setAlignment(Pos.CENTER_LEFT);
        for (Account acc : member.getAccounts()) {
            try {
                ImageView bankIcon = new ImageView(new Image(Objects.requireNonNull(
                        getClass().getResourceAsStream("/assets/icons/management_icon.png"))));
                bankIcon.setFitWidth(20); bankIcon.setFitHeight(20); bankIcon.setPreserveRatio(true);
                if (isSelected) bankIcon.setStyle("-fx-opacity: 1.0;");
                else bankIcon.setStyle("-fx-opacity: 0.4;");
                Tooltip.install(bankIcon, new Tooltip(acc.getDescription()));
                accountIcons.getChildren().add(bankIcon);
            } catch (Exception ignored) {}
        }

        card.getChildren().addAll(header, photo, addedLabel, accountIcons);

        // Klik — vyber člena a zobraz jeho accounty
        card.setOnMouseClicked(e -> selectMember(member));

        return card;
    }

    private void selectMember(FamilyMemberData member) {
        if (selectedMember != null && selectedMember.getUserId() == member.getUserId()) {
            // Deselect
            selectedMember = null;
            familyAccountsSection.setVisible(false);
            familyAccountsSection.setManaged(false);
            renderMembers();
            return;
        }
        selectedMember = member;
        renderMembers();
        loadMemberAccounts(member);
    }

    private void handleRemoveMember(FamilyMemberData member) {
        try {
            ServiceLocator.getFamilyService().removeFamilyMember(member.getUserId());
            if (selectedMember != null && selectedMember.getUserId() == member.getUserId()) {
                selectedMember = null;
                familyAccountsSection.setVisible(false);
                familyAccountsSection.setManaged(false);
            }
            loadFamilyMembers();
        } catch (Exception e) {
            String msg = e.getMessage();
            showFamilyError(msg != null && msg.startsWith("family.error.") ? msg : "error.db_error");
        }
    }

    // ════════════════════════════════════════════════════════════
    //  MEMBER ACCOUNTS
    // ════════════════════════════════════════════════════════════
    private void loadMemberAccounts(FamilyMemberData member) {
        familyAccountsTitle.setText(member.getFirstName() + " " + member.getLastName());

        selectedMemberDefaultAccounts = member.getAccounts().stream()
                .filter(a -> !a.isSavingAccount())
                .collect(Collectors.toList());
        selectedMemberSavingAccounts = member.getAccounts().stream()
                .filter(Account::isSavingAccount)
                .collect(Collectors.toList());

        defaultAccountsOffset = 0;
        savingAccountsOffset = 0;

        renderMemberDefaultAccounts();
        renderMemberSavingAccounts();

        familyAccountsSection.setVisible(true);
        familyAccountsSection.setManaged(true);
    }

    private void renderMemberDefaultAccounts() {
        familyDefaultAccountsRow.getChildren().clear();

        boolean canLeft = defaultAccountsOffset > 0;
        familyDefaultAccountsRow.getChildren().add(createArrow(true, canLeft, () -> {
            defaultAccountsOffset = Math.max(0, defaultAccountsOffset - 1);
            renderMemberDefaultAccounts();
        }));

        int from = defaultAccountsOffset;
        int to = Math.min(from + MAX_VISIBLE_ACCOUNT_CARDS, selectedMemberDefaultAccounts.size());
        for (int i = from; i < to; i++)
            familyDefaultAccountsRow.getChildren().add(createMemberAccountCard(selectedMemberDefaultAccounts.get(i)));

        boolean isLastPage = to >= selectedMemberDefaultAccounts.size();
        familyDefaultAccountsRow.getChildren().add(createArrow(false, !isLastPage, () -> {
            defaultAccountsOffset++;
            renderMemberDefaultAccounts();
        }));
    }

    private void renderMemberSavingAccounts() {
        familySavingAccountsRow.getChildren().clear();

        boolean canLeft = savingAccountsOffset > 0;
        familySavingAccountsRow.getChildren().add(createArrow(true, canLeft, () -> {
            savingAccountsOffset = Math.max(0, savingAccountsOffset - 1);
            renderMemberSavingAccounts();
        }));

        int from = savingAccountsOffset;
        int to = Math.min(from + MAX_VISIBLE_ACCOUNT_CARDS, selectedMemberSavingAccounts.size());
        for (int i = from; i < to; i++)
            familySavingAccountsRow.getChildren().add(createMemberAccountCard(selectedMemberSavingAccounts.get(i)));

        boolean isLastPage = to >= selectedMemberSavingAccounts.size();
        familySavingAccountsRow.getChildren().add(createArrow(false, !isLastPage, () -> {
            savingAccountsOffset++;
            renderMemberSavingAccounts();
        }));
    }

    private VBox createMemberAccountCard(Account account) {
        VBox card = new VBox(8);
        card.getStyleClass().add("account-card");
        HBox.setHgrow(card, Priority.ALWAYS);

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(getAccountDisplayName(account));
        title.getStyleClass().add("account-card-title");
        HBox.setHgrow(title, Priority.ALWAYS);
        header.getChildren().add(title);

        // Edit len pre saving účty
        if (account.isSavingAccount()) {
            Button editBtn = new Button();
            editBtn.getStyleClass().add("btn-icon-edit");
            editBtn.setMinWidth(36); editBtn.setMaxWidth(36); editBtn.setPrefWidth(36);
            editBtn.setMinHeight(36); editBtn.setMaxHeight(36); editBtn.setPrefHeight(36);
            try {
                ImageView icon = new ImageView(new Image(Objects.requireNonNull(
                        getClass().getResourceAsStream("/assets/icons/icon_edit.png"))));
                icon.setFitWidth(14); icon.setFitHeight(14); icon.setPreserveRatio(true);
                editBtn.setGraphic(icon);
            } catch (Exception ignored) {}
            // TODO: Adelka — updateSavingAccount validuje účet cez SessionManager.getAccountById()
            // čo vracia len účty prihláseného usera. Pre Family Manager treba buď:
            // a) pridať updateSavingAccountById ktorý skipuje SessionManager validáciu
            // b) alebo načítať účet priamo cez AccountRepository
            editBtn.setOnAction(e -> handleEditSavingAccount(account));
            header.getChildren().add(editBtn);
        }

        Label desc = new Label(account.getDescription());
        desc.getStyleClass().add("account-card-desc");

        Label created = new Label(Localization.get("management.accounts.created") + ": "
                + account.getCreatedAt().toLocalDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        created.getStyleClass().add("analytics-subtitle");

        Region vspacer = new Region();
        VBox.setVgrow(vspacer, Priority.ALWAYS);

        Label amount = new Label(formatCurrency(account.getCurrentBalance()));
        amount.getStyleClass().add("account-card-amount");

        card.getChildren().addAll(header, desc, created, vspacer, amount);

        if (account.isMainAccount() || account.isEmergencyFund()) {
            Label cantEdit = new Label(Localization.get("management.accounts.cannot_delete"));
            cantEdit.getStyleClass().add("modal-error-label");
            card.getChildren().add(cantEdit);
        }

        return card;
    }

    // ════════════════════════════════════════════════════════════
    //  EDIT SAVING ACCOUNT MODAL
    // ════════════════════════════════════════════════════════════
    private void handleEditSavingAccount(Account account) {
        editingAccount = account;
        clearEditSavingModalError();

        editSavingDescField.setText(account.getDescription());
        editSavingDatePicker.setConverter(new javafx.util.StringConverter<>() {
            private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            @Override public String toString(LocalDate d) { return d != null ? d.format(fmt) : ""; }
            @Override public LocalDate fromString(String s) {
                return (s != null && !s.isEmpty()) ? LocalDate.parse(s, fmt) : null;
            }
        });

        try {
            Optional<SavingGoal> goalOpt = ServiceLocator.getAccountService().getSavingGoal(account.getId());
            if (goalOpt.isPresent()) {
                SavingGoal goal = goalOpt.get();
                editSavingGoalField.setText(String.format("%.2f", goal.getTargetAmount()));
                editSavingDatePicker.setValue(goal.getTargetDate().toLocalDate());
            } else {
                editSavingGoalField.clear();
                editSavingDatePicker.setValue(null);
            }
        } catch (Exception e) {
            editSavingGoalField.clear();
            editSavingDatePicker.setValue(null);
        }

        final boolean[] p = {false};
        editSavingModalOverlay.setOnMousePressed(e -> p[0] = e.getTarget() == editSavingModalOverlay);
        editSavingModalOverlay.setOnMouseReleased(e -> {
            if (p[0] && e.getTarget() == editSavingModalOverlay) closeEditSavingModal();
        });
        editSavingModalOverlay.setVisible(true);
        editSavingModalOverlay.setManaged(true);
    }

    @FXML private void onEditSavingConfirm() {
        clearEditSavingModalError();
        if (editingAccount == null) { closeEditSavingModal(); return; }

        String desc = editSavingDescField.getText().trim();
        String goalText = editSavingGoalField.getText().trim();

        if (desc.isEmpty()) { showEditSavingModalError("account.error.description_required"); return; }
        if (goalText.isEmpty()) { showEditSavingModalError("account.error.goal_amount_invalid"); return; }
        if (editSavingDatePicker.getValue() == null) { showEditSavingModalError("account.error.goal_date_invalid"); return; }

        double goalAmount;
        try {
            goalAmount = Double.parseDouble(goalText.replace(",", "."));
        } catch (NumberFormatException e) {
            showEditSavingModalError("account.error.goal_amount_invalid"); return;
        }

        try {
            ServiceLocator.getAccountService().updateSavingAccount(
                    editingAccount.getId(), desc, goalAmount, editSavingDatePicker.getValue());
            closeEditSavingModal();
            // Reload member accounts
            if (selectedMember != null) loadMemberAccounts(selectedMember);
        } catch (Exception e) {
            String msg = e.getMessage();
            showEditSavingModalError(msg != null && msg.startsWith("account.error.") ? msg : "error.db_error");
        }
    }

    @FXML private void onEditSavingClose() { closeEditSavingModal(); }
    private void closeEditSavingModal() {
        editingAccount = null;
        editSavingModalOverlay.setVisible(false);
        editSavingModalOverlay.setManaged(false);
    }
    private void showEditSavingModalError(String key) { editSavingModalErrorLabel.setText(Localization.get(key)); editSavingModalErrorLabel.setVisible(true); editSavingModalErrorLabel.setManaged(true); }
    private void clearEditSavingModalError() { editSavingModalErrorLabel.setVisible(false); editSavingModalErrorLabel.setManaged(false); }

    // ════════════════════════════════════════════════════════════
    //  ADD USER MODAL
    // ════════════════════════════════════════════════════════════
    @FXML private void handleAddUser() {
        clearAddUserModalError();
        addUserEmailField.clear();
        final boolean[] p = {false};
        addUserModalOverlay.setOnMousePressed(e -> p[0] = e.getTarget() == addUserModalOverlay);
        addUserModalOverlay.setOnMouseReleased(e -> {
            if (p[0] && e.getTarget() == addUserModalOverlay) closeAddUserModal();
        });
        addUserModalOverlay.setVisible(true);
        addUserModalOverlay.setManaged(true);
    }

    @FXML private void onAddUserConfirm() {
        clearAddUserModalError();
        String email = addUserEmailField.getText().trim();
        if (email.isEmpty()) { showAddUserModalError("family.error.email_required"); return; }

        try {
            ServiceLocator.getFamilyService().sendFamilyRequest(email);
            closeAddUserModal();
            // Zobraz info že request bol odoslaný
            showFamilyInfo("family.request.sent");
        } catch (Exception e) {
            String msg = e.getMessage();
            showAddUserModalError(msg != null && msg.startsWith("family.error.") ? msg : "error.db_error");
        }
    }

    @FXML private void onAddUserClose() { closeAddUserModal(); }
    private void closeAddUserModal() { addUserModalOverlay.setVisible(false); addUserModalOverlay.setManaged(false); }
    private void showAddUserModalError(String key) { addUserModalErrorLabel.setText(Localization.get(key)); addUserModalErrorLabel.setVisible(true); addUserModalErrorLabel.setManaged(true); }
    private void clearAddUserModalError() { addUserModalErrorLabel.setVisible(false); addUserModalErrorLabel.setManaged(false); }

    // ════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════
    private VBox createArrow(boolean isLeft, boolean visible, Runnable onClick) {
        VBox arrow = new VBox();
        arrow.setPadding(new Insets(0, 8, 0, 8));
        arrow.getStyleClass().add("accounts-arrow");
        arrow.setAlignment(Pos.CENTER);
        try {
            String path = isLeft ? "/assets/icons/icon_arrow_left.png" : "/assets/icons/icon_arrow_right.png";
            ImageView icon = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream(path))));
            icon.setFitWidth(24); icon.setFitHeight(24); icon.setPreserveRatio(true);
            icon.getStyleClass().add("accounts-arrow-icon");
            arrow.getChildren().add(icon);
        } catch (Exception ignored) {}
        arrow.setOnMouseClicked(e -> onClick.run());
        arrow.setVisible(visible); arrow.setManaged(visible);
        return arrow;
    }

    private void showFamilyError(String key) {
        familyErrorLabel.setText(Localization.get(key));
        familyErrorLabel.setVisible(true);
        familyErrorLabel.setManaged(true);
    }

    private void showFamilyInfo(String key) {
        familyErrorLabel.setText(Localization.get(key));
        familyErrorLabel.setStyle("-fx-text-fill: #2563EB;");
        familyErrorLabel.setVisible(true);
        familyErrorLabel.setManaged(true);
    }

    private String getAccountDisplayName(Account account) {
        if (account.isMainAccount()) return Localization.get("dashboard.account.main");
        if (account.isEmergencyFund()) return Localization.get("dashboard.account.emergency");
        if (account.isSavingAccount()) return Localization.get("dashboard.account.saving");
        return Localization.get("dashboard.account.default");
    }

    private String formatCurrency(double value) {
        return String.format("€%,.2f", value);
    }
}