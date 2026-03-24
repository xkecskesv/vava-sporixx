package sk.sporixx.ui;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;

public class LoginController {
    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if(email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please, fill in all fields.");
            errorLabel.setVisible(true);
            return;
        }

        if(!email.contains("@")) {
            errorLabel.setText("Please, enter a valid email address.");
            errorLabel.setVisible(true);
            return;
        }

        //boolean success = userService.login(email,password);
    }

    @FXML
    private void handleNewAccount(){
        System.out.println("Prejdi na registráciu");
    }
}
