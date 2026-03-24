package sk.sporixx.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.ResourceBundle;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private ResourceBundle resources;

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        //TODO: TU bodla toho aky mi pride exception zo service vrstvy vyhodim error label
        // TODO: authService.login(email, password);
    }

    @FXML
    private void handleNewAccount() {

        try {
            SceneManager.switchTo("register.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}