package demo;

import demo.Enums.UserRole;
import demo.PresentationLayer.Boundaries.NewUserBoundary;
import demo.PresentationLayer.Boundaries.UserBoundary;
import demo.PresentationLayer.Boundaries.UserNameBoundary;
import demo.DataAccessLayer.IDs.UserID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserControllerTests {

    private int port;
    private String appName;

    private RestClient usersClient;
    private RestClient adminClient;

    private final String ADMIN_CLEANUP_EMAIL = "admin.cleanup@example.com";
    private NewUserBoundary adminCleanupNewUser;
    private UserID adminCleanupUserID;


    @LocalServerPort
    public void setPort(int port) {
        this.port = port;
    }

    @Value("${spring.application.name:defaultApp}")
    public void setAppName(String appName) {
        this.appName = appName;
    }

    @BeforeEach
    public void setUp() {
        this.usersClient = RestClient.create("http://localhost:" + this.port + "/ambient-intelligence/users");
        this.adminClient = RestClient.create("http://localhost:" + this.port + "/ambient-intelligence/admin");

        adminCleanupNewUser = new NewUserBoundary();
        adminCleanupNewUser.setEmail(ADMIN_CLEANUP_EMAIL);
        adminCleanupNewUser.setRole(UserRole.ADMIN);
        adminCleanupNewUser.setUsername(new UserNameBoundary("Admin", "Cleanup"));
        adminCleanupNewUser.setAvatar("avatar_cleanup.png");

        // Attempt to create the cleanup admin user. This user is essential for cleaning.
        try {
            usersClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(adminCleanupNewUser)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            // Ignore if it already exists (e.g., 400 Bad Request due to duplicate email)
            if (e.getStatusCode() != HttpStatus.BAD_REQUEST && e.getStatusCode() != HttpStatus.CONFLICT) {
                System.err.println("Warning: Could not create initial cleanup admin, proceeding with cleanup: " + e.getMessage());
            }
        }

        // Perform cleanup using admin credentials (even if creation above failed, it might exist from a previous run)
        // These calls might fail if the admin user doesn't exist or has no permission yet,
        // but we try our best. The re-creation of admin user afterwards is key.
        try {
            adminClient.delete().uri("/commands?userSystemID={sysId}&userEmail={email}", this.appName, ADMIN_CLEANUP_EMAIL).retrieve().toBodilessEntity();
        } catch (Exception e) { /* ignore cleanup errors */ }
        try {
            adminClient.delete().uri("/objects?userSystemID={sysId}&userEmail={email}", this.appName, ADMIN_CLEANUP_EMAIL).retrieve().toBodilessEntity();
        } catch (Exception e) { /* ignore cleanup errors */ }
        try {
            adminClient.delete().uri("/users?userSystemID={sysId}&userEmail={email}", this.appName, ADMIN_CLEANUP_EMAIL).retrieve().toBodilessEntity();
        } catch (Exception e) { /* ignore cleanup errors */ }


        // Re-create the cleanup admin user to ensure it exists for the test and AfterEach
        UserBoundary createdAdmin = usersClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(adminCleanupNewUser)
                .retrieve()
                .body(UserBoundary.class);
        assertThat(createdAdmin).isNotNull();
        this.adminCleanupUserID = createdAdmin.getUserId();
        assertThat(this.adminCleanupUserID.getSystemID()).isEqualTo(this.appName);
    }

    @AfterEach
    public void tearDown() {
        try {
            adminClient.delete().uri("/commands?userSystemID={sysId}&userEmail={email}", this.adminCleanupUserID.getSystemID(), this.adminCleanupUserID.getEmail()).retrieve().toBodilessEntity();
        } catch (Exception e) { /* ignore cleanup errors */ }
        try {
            adminClient.delete().uri("/objects?userSystemID={sysId}&userEmail={email}", this.adminCleanupUserID.getSystemID(), this.adminCleanupUserID.getEmail()).retrieve().toBodilessEntity();
        } catch (Exception e) { /* ignore cleanup errors */ }
        try {
            adminClient.delete().uri("/users?userSystemID={sysId}&userEmail={email}", this.adminCleanupUserID.getSystemID(), this.adminCleanupUserID.getEmail()).retrieve().toBodilessEntity();
        } catch (Exception e) { /* ignore cleanup errors */ }
    }

    @Test
    @DisplayName("Test Create New User Successfully")
    public void testCreateNewUserSuccessfully() {
        // GIVEN server is up and clean
        NewUserBoundary newUser = new NewUserBoundary();
        newUser.setEmail("test.user@example.com");
        newUser.setRole(UserRole.END_USER);
        newUser.setUsername(new UserNameBoundary("Test", "User"));
        newUser.setAvatar("test_avatar.png");

        // WHEN I POST to /ambient-intelligence/users
        UserBoundary createdUser = usersClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(newUser)
                .retrieve()
                .body(UserBoundary.class);

        // THEN a new user is created with correct details
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getUserId().getEmail()).isEqualTo(newUser.getEmail());
        assertThat(createdUser.getUserId().getSystemID()).isEqualTo(this.appName);
        assertThat(createdUser.getRole()).isEqualTo(newUser.getRole());
        assertThat(createdUser.getUsername().getFirst()).isEqualTo(newUser.getUsername().getFirst());
        assertThat(createdUser.getUsername().getLast()).isEqualTo(newUser.getUsername().getLast());
        assertThat(createdUser.getAvatar()).isEqualTo(newUser.getAvatar());
    }

    @Test
    @DisplayName("Test Create New User Fails With Invalid Email")
    public void testCreateNewUserFailsWithInvalidEmail() {
        NewUserBoundary newUser = new NewUserBoundary();
        newUser.setEmail("invalid-email");
        newUser.setRole(UserRole.END_USER);
        newUser.setUsername(new UserNameBoundary("Test", "User"));
        newUser.setAvatar("test_avatar.png");

        assertThatThrownBy(() -> usersClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(newUser)
                .retrieve()
                .body(UserBoundary.class))
                .isInstanceOf(HttpClientErrorException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST);
    }
    
    @Test
    @DisplayName("Test Create New User Fails With Missing Role")
    public void testCreateNewUserFailsWithMissingRole() {
        NewUserBoundary newUser = new NewUserBoundary();
        newUser.setEmail("test.role@example.com");
        // newUser.setRole(UserRole.END_USER); // Missing role
        newUser.setUsername(new UserNameBoundary("Test", "User"));
        newUser.setAvatar("test_avatar.png");

        assertThatThrownBy(() -> usersClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(newUser)
                .retrieve()
                .body(UserBoundary.class))
                .isInstanceOf(HttpClientErrorException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST);
    }


    @Test
    @DisplayName("Test Login Valid User Successfully")
    public void testLoginValidUserSuccessfully() {
        // GIVEN a user exists
        NewUserBoundary newUserDetails = new NewUserBoundary();
        newUserDetails.setEmail("login.user@example.com");
        newUserDetails.setRole(UserRole.OPERATOR);
        newUserDetails.setUsername(new UserNameBoundary("Login", "User"));
        newUserDetails.setAvatar("login_avatar.png");

        UserBoundary existingUser = usersClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(newUserDetails)
                .retrieve()
                .body(UserBoundary.class);
        assertThat(existingUser).isNotNull();

        // WHEN I GET /ambient-intelligence/users/login/{systemID}/{userEmail}
        UserBoundary loggedInUser = usersClient.get()
                .uri("/login/{systemID}/{userEmail}", existingUser.getUserId().getSystemID(), existingUser.getUserId().getEmail())
                .retrieve()
                .body(UserBoundary.class);

        // THEN the correct user details are returned
        assertThat(loggedInUser).isNotNull();
        assertThat(loggedInUser.getUserId().getEmail()).isEqualTo(existingUser.getUserId().getEmail());
        assertThat(loggedInUser.getRole()).isEqualTo(existingUser.getRole());
    }

    @Test
    @DisplayName("Test Login Non-Existent User Fails")
    public void testLoginNonExistentUserFails() {
        // WHEN I GET /ambient-intelligence/users/login/{systemID}/{userEmail} for a non-existent user
        // THEN a 403 Forbidden is returned (as per UserServiceImpl logic for non-existent user during login)
        assertThatThrownBy(() -> usersClient.get()
                .uri("/login/{systemID}/{userEmail}", this.appName, "non.existent@example.com")
                .retrieve()
                .body(UserBoundary.class))
                .isInstanceOf(HttpClientErrorException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Test Update User Details Successfully")
    public void testUpdateUserDetailsSuccessfully() {
        // GIVEN a user exists
        NewUserBoundary newUserDetails = new NewUserBoundary();
        newUserDetails.setEmail("update.user@example.com");
        newUserDetails.setRole(UserRole.END_USER);
        newUserDetails.setUsername(new UserNameBoundary("OriginalFirst", "OriginalLast"));
        newUserDetails.setAvatar("original_avatar.png");

        UserBoundary existingUser = usersClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(newUserDetails)
                .retrieve()
                .body(UserBoundary.class);
        assertThat(existingUser).isNotNull();

        UserBoundary updatePayload = new UserBoundary();
        updatePayload.setUsername(new UserNameBoundary("UpdatedFirst", "UpdatedLast"));
        updatePayload.setAvatar("updated_avatar.png");
        updatePayload.setRole(UserRole.OPERATOR); // Also updating role

        // WHEN I PUT to /ambient-intelligence/users/{systemID}/{userEmail}
        usersClient.put()
                .uri("/{systemID}/{userEmail}", existingUser.getUserId().getSystemID(), existingUser.getUserId().getEmail())
                .contentType(MediaType.APPLICATION_JSON)
                .body(updatePayload)
                .retrieve()
                .toBodilessEntity();

        // THEN the user details are updated
        UserBoundary updatedUser = usersClient.get()
                .uri("/login/{systemID}/{userEmail}", existingUser.getUserId().getSystemID(), existingUser.getUserId().getEmail())
                .retrieve()
                .body(UserBoundary.class);

        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.getUsername().getFirst()).isEqualTo("UpdatedFirst");
        assertThat(updatedUser.getUsername().getLast()).isEqualTo("UpdatedLast");
        assertThat(updatedUser.getAvatar()).isEqualTo("updated_avatar.png");
        assertThat(updatedUser.getRole()).isEqualTo(UserRole.OPERATOR);
    }

    @Test
    @DisplayName("Test Update Non-Existent User Fails")
    public void testUpdateNonExistentUserFails() {
        UserBoundary updatePayload = new UserBoundary();
        updatePayload.setUsername(new UserNameBoundary("UpdatedFirst", "UpdatedLast"));

        // WHEN I PUT to /ambient-intelligence/users/{systemID}/{userEmail} for a non-existent user
        // THEN a 404 Not Found is returned
        assertThatThrownBy(() -> usersClient.put()
                .uri("/{systemID}/{userEmail}", this.appName, "non.existent.update@example.com")
                .contentType(MediaType.APPLICATION_JSON)
                .body(updatePayload)
                .retrieve()
                .toBodilessEntity())
                .isInstanceOf(HttpClientErrorException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.NOT_FOUND);
    }
}