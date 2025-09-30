package demo.DataAccessLayer.IDs;

public class UserID {
    private String email;
    private String systemId;

    public UserID() {
    }

    public UserID(String email, String systemId) {
        this.email = email;
        this.systemId = systemId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSystemID() {
        return systemId;
    }

    public void setSystemID(String systemId) {
        this.systemId = systemId;
    }

    @Override
    public String toString() {
        return "{email=" + email + ", systemId=" + systemId + "}";
    }
}
