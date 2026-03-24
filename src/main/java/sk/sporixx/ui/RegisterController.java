package sk.sporixx.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;


public class RegisterController {

    @FXML public PasswordField repeatPasswordField;
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    private void handleRegister() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String repeatPassword = repeatPasswordField.getText();

        //TODO: TU bodla toho aky mi pride exception zo service vrstvy vyhodim error label
        // TODO: userService.register(name, email, password, repeatPassword);
        //TODO: potom k tomu napisat toto: errorLabel.setText(Localization.get("error.empty.fields"));
    }

    @FXML
    private void handleHasAccount() {
        try {
            SceneManager.switchTo("login.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}