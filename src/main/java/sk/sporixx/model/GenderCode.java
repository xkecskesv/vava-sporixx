package sk.sporixx.model;

/**
 * Canonical gender codes persisted in the users table.
 */
public final class GenderCode {

    public static final String MALE = "M";
    public static final String FEMALE = "F";
    public static final String UNKNOWN = "ONHSR";

    private GenderCode() {
        throw new UnsupportedOperationException("Utility class");
    }
}

