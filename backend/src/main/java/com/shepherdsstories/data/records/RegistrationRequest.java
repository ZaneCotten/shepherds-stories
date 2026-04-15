package com.shepherdsstories.data.records;

public record RegistrationRequest(
        String email,
        String role,          // MISSIONARY or SUPPORTER
        String authProvider,  // As of right now, only GOOGLE
        String displayName,   // Social display name (from provider)
        String firstName,     // Social given name (from provider)
        String lastName,      // Social family name (from provider)
        String profilePictureUrl // Social profile picture (from provider)
) {
}
