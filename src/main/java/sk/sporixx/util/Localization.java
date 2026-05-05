package sk.sporixx.util;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class Localization {

    private static ResourceBundle bundle;
    private static String currentLanguage = "en";

    public static void load(String language) throws Exception {
        currentLanguage = language;
        bundle = new PropertyResourceBundle(
                new InputStreamReader(
                        Objects.requireNonNull(Localization.class.getResourceAsStream("/i18n/messages_" + language + ".properties")),
                        StandardCharsets.UTF_8
                )
        );
    }

    public static void setLanguage(String language) {
        try {
            load(language);
        } catch (Exception e) {
            try {
                load("en");
            } catch (Exception fallbackException) {
                throw new IllegalStateException("Unable to load any localization bundle", fallbackException);
            }
        }
    }

    public static String get(String key) {
        return bundle.getString(key);
    }

    public static ResourceBundle getBundle() {
        return bundle;
    }

    public static String getCurrentLanguage() {
        return currentLanguage;
    }
}
