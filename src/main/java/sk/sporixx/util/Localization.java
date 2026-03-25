package sk.sporixx.util;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class Localization {

    private static ResourceBundle bundle;

    public static void load(String language) throws Exception {
        bundle = new PropertyResourceBundle(
                new InputStreamReader(
                        Objects.requireNonNull(Localization.class.getResourceAsStream("/i18n/messages_" + language + ".properties")),
                        StandardCharsets.UTF_8
                )
        );
    }

    public static String get(String key) {
        return bundle.getString(key);
    }

    public static ResourceBundle getBundle() {
        return bundle;
    }
}
