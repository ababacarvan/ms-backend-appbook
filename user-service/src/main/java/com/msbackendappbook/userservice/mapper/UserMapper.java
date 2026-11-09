package com.msbackendappbook.userservice.mapper;

import com.msbackendappbook.userservice.model.dto.UserDTO;
import com.msbackendappbook.userservice.model.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserDTO mapToDTO(User user) {
        if (user == null) {
            return null;
        }

        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .dateOfBirth(user.getDateOfBirth())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .active(user.isActive())
                .emailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }

    public User mapToEntity(UserDTO userDTO) {
        if (userDTO == null) {
            return null;
        }

        return User.builder()
                .id(userDTO.getId())
                .email(userDTO.getEmail())
                .firstName(userDTO.getFirstName())
                .lastName(userDTO.getLastName())
                .phoneNumber(userDTO.getPhoneNumber())
                .dateOfBirth(userDTO.getDateOfBirth())
                .isActive(userDTO.isActive())
                .emailVerified(userDTO.isEmailVerified())
                .build();
    }
}