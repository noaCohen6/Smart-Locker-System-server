package demo.PresentationLayer.Boundaries;

import demo.Enums.UserRole;
import demo.DataAccessLayer.IDs.UserID;

public class UserBoundary {
    private UserID userId;
    private UserRole role;
    private UserNameBoundary username;
    private String avatar;

    public UserBoundary() {
    }
    public  UserBoundary(UserID userId, UserRole role, UserNameBoundary username, String avatar) {
        this.userId = userId;
        this.role = role;
        this.username = username;
        this.avatar = avatar;
    }

    public UserID getUserId() {
        return userId;
    }

    public void setUserId(UserID userId) {
        this.userId = userId;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public UserNameBoundary getUsername() {
        return username;
    }

    public void setUsername(UserNameBoundary username) {
        this.username = username;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    @Override
    public String toString() {
        return "UserBoundary{" +
                "userID=" + userId +
                ", role=" + role +
                ", username=" + username +
                ", avatar='" + avatar + '\'' +
                '}';
    }
}