package bff.dto;

public record UserDto(
        Long id,
        String login,
        String role,
        String fullName,
        String birthDate,
        String avatarUrl,
        boolean deleted,
        boolean blocked) {
}
