package sk.sporixx.service;

import sk.sporixx.dto.AdminUserData;
import sk.sporixx.model.Role;
import sk.sporixx.model.User;
import sk.sporixx.repository.UserRepository;

import java.util.List;

/**
 * Default admin service implementation.
 */
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;

    public AdminServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<AdminUserData> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    private AdminUserData toDto(User user) {
        return AdminUserData.builder()
                .id(user.getId())
                .name((safe(user.getFirstName()) + " " + safe(user.getLastName())).trim())
                .email(safe(user.getEmail()))
                .familyManager(user.getRole() == Role.FAMILY_MANAGER)
                .active(user.isActive())
                .build();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

