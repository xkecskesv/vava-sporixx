package sk.sporixx.service;

import lombok.Getter;
import sk.sporixx.model.Account;
import sk.sporixx.model.Role;
import sk.sporixx.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Správca session prihláseného používateľa.
 * Singleton
 * Kolekcie: ArrayList pre účty, Collections.unmodifiableList() pre read-only prístup
 */
public class SessionManager {

    private SessionManager() {
        this.accounts = new ArrayList<>();
    }

    private static class Holder {
        private static final SessionManager INSTANCE = new SessionManager();
    }

    public static SessionManager getInstance() {
        return Holder.INSTANCE;
    }

    @Getter
    private User currentUser;
    private List<Account> accounts;

    /**
     * Nastaví session po úspešnom prihlásení.
     * @param user overený používateľ
     * @param accounts zoznam jeho účtov
     * @throws IllegalArgumentException ak user je null
     */
    public void setSession(User user, List<Account> accounts) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null when setting session");
        }
        this.currentUser = user;
        this.accounts = (accounts != null) ? new ArrayList<>(accounts) : new ArrayList<>();
    }

    /** Vyčistí session pri odhlásení. */
    public void clearSession() {
        this.currentUser = null;
        this.accounts = new ArrayList<>();
    }

    /** Vráti ID prihláseného používateľa, alebo -1 ak nikto nie je prihlásený.
     */
    public int getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : -1;
    }

    /**
     * Vracia NEMODIFIKOVATEĽNÝ zoznam účtov.
     */
    public List<Account> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.getRole() == Role.ADMIN;
    }

    public boolean isFamilyManager() {
        return currentUser != null && currentUser.getRole() == Role.FAMILY_MANAGER;
    }

    public boolean isUser() {
        return currentUser != null && currentUser.getRole() == Role.USER;
    }

    /**
     * Kontroluje, či prihlásený má aspoň danú úroveň prístupu.
     * ADMIN(3) > FAMILY_MANAGER(2) > USER(1)
     */
    public boolean hasMinimumRole(Role minimumRole) {
        if (currentUser == null || currentUser.getRole() == null) {
            return false;
        }
        return currentUser.getRole().getAccessLevel() >= minimumRole.getAccessLevel();
    }
}