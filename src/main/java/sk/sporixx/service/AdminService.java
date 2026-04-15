package sk.sporixx.service;

import sk.sporixx.dto.AdminUserData;

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
}

