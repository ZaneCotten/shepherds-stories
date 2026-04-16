package com.shepherdsstories.dtos;

import com.shepherdsstories.data.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.shepherdsstories.utils.ValidationConstants.PASSWORD_MAX_LENGTH;
import static com.shepherdsstories.utils.ValidationConstants.PASSWORD_MIN_LENGTH;

@Data
public class RegistrationRequestDTO {

    // --- Core User Fields ---
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_LENGTH, message = "Password must be at least 8 characters")
    private String password;

    @NotNull(message = "Role is required")
    private Role role;

    // --- Supporter-Specific Fields ---
    // (Required if role is SUPPORTER)
    private String firstName;
    private String lastName;

    // --- Missionary-Specific Fields ---
    // (Required if role is MISSIONARY)
    private String displayName;
    private String region;
    private String biography;
    private String profilePictureUrl;
}