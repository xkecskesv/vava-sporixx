package sk.sporixx.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import sk.sporixx.util.Localization;

public class SceneManager {

    private static Stage primaryStage;

    public static void setStage(Stage stage) {
        primaryStage = stage;
    }

    public static void switchTo(String fxmlFile) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                SceneManager.class.getResource("/fxml/" + fxmlFile),
                Localization.getBundle()
        );

        Parent root = loader.load();
        primaryStage.getScene().setRoot(root);
    }
}