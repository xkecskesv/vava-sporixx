package sk.sporixx.service;

import sk.sporixx.dto.AdminUserData;
import sk.sporixx.model.User;

import java.util.List;

/**
 * Rozhranie pre administrátorské operácie v paneli správy používateľov.
 */
public interface AdminService {

    /**
     * Načíta všetkých používateľov pre tabuľku v administrátorskom prehľade.
     *
     * @return dáta používateľov pre tabuľku
     */
    List<AdminUserData> getAllUsers();

    /**
     * Aktualizuje editovateľné údaje vybraného používateľa.
     *
     * @param user objekt s ID používateľa a upravenými hodnotami
     * @return aktualizovaný používateľ uložený v úložisku
     */
    User updateUser(User user);

    /**
     * Deaktivuje účty vybraného používateľa.
     *
     * @param user objekt s ID cieľového používateľa
     * @return používateľ po zmene aktívneho stavu
     */
    User deactivateUser(User user);

    /**
     * Znovu aktivuje účty vybraného používateľa.
     *
     * @param user objekt s ID cieľového používateľa
     * @return používateľ po zmene aktívneho stavu
     */
    User activateUser(User user);

    /**
     * Natrvalo odstráni vybraného používateľa.
     *
     * @param user objekt s ID cieľového používateľa
     */
    void deleteUser(User user);

    /**
     * Zmení heslo aktuálne prihláseného administrátora.
     *
     * @param user objekt používateľa; musí odkazovať na aktuálne prihláseného administrátora
     * @param oldPassword aktuálne heslo
     * @param newPassword nové heslo
     */
    void changeOwnPassword(User user, String oldPassword, String newPassword);
}

