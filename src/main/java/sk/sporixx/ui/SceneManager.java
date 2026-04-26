package sk.sporixx.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import sk.sporixx.util.Localization;

public class SceneManager {

    private static Stage primaryStage;
    private static String currentFxml;

    public static void setStage(Stage stage) {
        primaryStage = stage;
    }

    public static void setCurrentScene(String fxmlFile) {
        currentFxml = fxmlFile;
    }

    public static void switchTo(String fxmlFile) throws Exception {
        currentFxml = fxmlFile;
        FXMLLoader loader = new FXMLLoader(
                SceneManager.class.getResource("/fxml/" + fxmlFile),
                Localization.getBundle()
        );

        Parent root = loader.load();
        primaryStage.getScene().setRoot(root);
    }

    public static void reloadCurrentScene() throws Exception {
        if (currentFxml == null || currentFxml.isBlank()) {
            return;
        }
        switchTo(currentFxml);
    }
}