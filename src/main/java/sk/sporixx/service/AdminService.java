package sk.sporixx.service;

import sk.sporixx.dto.AdminUserData;
import sk.sporixx.model.User;

import java.util.List;

/**
 * Admin operations for the dedicated admin panel.
 */
public interface AdminService {

    /**
     * Loads all users for admin overview.
     *
     * @return users table data
     */
    List<AdminUserData> getAllUsers();

    /**
     * Updates editable user fields for the selected user.
     *
     * @param user payload with user id and edited values
     * @return updated persisted user
     */
    User updateUser(User user);

    /**
     * Marks selected user account as inactive.
     *
     * @param user payload with target user id
     * @return updated persisted user
     */
    User deactivateUser(User user);

    /**
     * Re-activates selected user account set.
     *
     * @param user payload with target user id
     * @return updated persisted user view
     */
    User activateUser(User user);

    /**
     * Changes password of currently logged in admin (self-edit).
     *
     * @param user selected user payload; must reference currently logged admin
     * @param oldPassword current password
     * @param newPassword new password
     */
    void changeOwnPassword(User user, String oldPassword, String newPassword);
}

