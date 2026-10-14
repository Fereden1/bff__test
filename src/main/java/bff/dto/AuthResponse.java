package bff.dto;

public class AuthResponse {
    private String accessToken;
    private long expiresInSeconds;
    private UserDto user;

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public long getExpiresInSeconds() { return expiresInSeconds; }
    public void setExpiresInSeconds(long expiresInSeconds) { this.expiresInSeconds = expiresInSeconds; }
    public UserDto getUser() { return user; }
    public void setUser(UserDto user) { this.user = user; }
}
