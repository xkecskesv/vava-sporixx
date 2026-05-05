package sk.sporixx.util;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;

import java.io.FileInputStream;
import java.util.Objects;

public class AvatarUtil {

    public static void apply(ImageView imageView, String photoPath, double size, Class<?> fallbackClass) {
        double radius = size / 2.0;
        imageView.setFitWidth(size);
        imageView.setFitHeight(size);
        imageView.setPreserveRatio(false);

        Circle clip = new Circle(radius, radius, radius);
        imageView.setClip(clip);

        if (photoPath != null && !photoPath.isBlank()) {
            try {
                imageView.setImage(new Image(new FileInputStream(photoPath)));
                return;
            } catch (Exception ignored) {}
        }

        try {
            imageView.setImage(new Image(Objects.requireNonNull(
                    fallbackClass.getResourceAsStream("/assets/icons/default_profile_picture.png"))));
        } catch (Exception ignored) {}
    }
}