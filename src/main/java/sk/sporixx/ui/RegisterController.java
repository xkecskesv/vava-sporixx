package sk.sporixx.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.ResourceBundle;

public class RegisterController {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private ResourceBundle resources;

    @FXML
    private void handleRegister() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        //TODO: TU bodla toho aky mi pride exception zo service vrstvy vyhodim error label

        // TODO: userService.register(name, email, password);
        System.out.println("Register: " + email);
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