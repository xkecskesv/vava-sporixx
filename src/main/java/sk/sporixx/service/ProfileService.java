package sk.sporixx.service;

/**
 * Rozhranie pre operácie profilu aktuálne prihláseného používateľa.
 *
 * @author Viktória Kecskés
 */
public interface ProfileService {

    /**
     * Aktualizuje identifikačné údaje profilu.
     *
     * @param firstName nové meno
     * @param lastName nové priezvisko
     * @param email nová e-mailová adresa
     * @param gender surová hodnota pohlavia z UI
     * @param isParent či je používateľ v profile označený ako rodinný manažér
     */
    void updateProfile(String firstName, String lastName, String email, String gender, boolean isParent);

    /**
     * Zmení heslo po overení aktuálneho hesla a bezpečnostných pravidiel.
     *
     * @param oldPassword aktuálne heslo v otvorenom texte
     * @param newPassword nové heslo v otvorenom texte
     */
    void changePassword(String oldPassword, String newPassword);

    /**
     * Uloží cestu k vybranej profilovej fotografii.
     *
     * @param photoPath absolútna cesta k fotografii
     */
    void updateProfilePhoto(String photoPath);

    /**
     * Prevedie surovú hodnotu pohlavia na lokalizovaný text pre UI.
     *
     * @param rawGender surová uložená hodnota
     * @return lokalizovaná zobrazovaná hodnota alebo náhradná hodnota {@code "-"}
     */
    String toDisplayGender(String rawGender);
}

