package demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.springframework.core.ParameterizedTypeReference;

import demo.Enums.UserRole;
import demo.PresentationLayer.Boundaries.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
public class AdminControllerTests {

    private int port;
    private String appName;
    private RestClient adminClient;
    private RestClient usersClient;
    private RestClient objectsClient;
    private RestClient commandsClient;

    private final String ADMIN_EMAIL = "admin.test@example.com";
    private final String REGULAR_USER_EMAIL = "regular.test@example.com";
    private final String OPERATOR_EMAIL = "operator.test@example.com";

    private UserBoundary adminUser;
    private UserBoundary regularUser;
    private UserBoundary operatorUser;


    @Value("${server.port:8084}")
    public void setPort(int port) {
        this.port = port;
    }

    @Value("${spring.application.name:defaultApp}")
    public void setAppName(String appName) {
        this.appName = appName;
    }

    @BeforeEach
    public void setUp() {
        this.adminClient = RestClient.create("http://localhost:" + this.port + "/ambient-intelligence/admin");
        this.usersClient = RestClient.create("http://localhost:" + this.port + "/ambient-intelligence/users");
        this.objectsClient = RestClient.create("http://localhost:" + this.port + "/ambient-intelligence/objects");
        this.commandsClient = RestClient.create("http://localhost:" + this.port + "/ambient-intelligence/commands");

        // Clean up any existing data first
        try {
            // Try cleanup with a known admin if exists
            adminClient.delete().uri("/commands?userSystemID={sysId}&userEmail={email}", this.appName, ADMIN_EMAIL).retrieve().toBodilessEntity();
            adminClient.delete().uri("/objects?userSystemID={sysId}&userEmail={email}", this.appName, ADMIN_EMAIL).retrieve().toBodilessEntity();
            adminClient.delete().uri("/users?userSystemID={sysId}&userEmail={email}", this.appName, ADMIN_EMAIL).retrieve().toBodilessEntity();
        } catch (Exception e) {
            // Ignore cleanup failures during setup
        }

        // Create admin user
        NewUserBoundary adminNewUser = new NewUserBoundary(
                ADMIN_EMAIL,
                UserRole.ADMIN,
                new UserNameBoundary("Admin", "User"),
                "admin_avatar.png"
        );
        NewUserBoundary OperatorNewUser = new NewUserBoundary(
                OPERATOR_EMAIL,
                UserRole.OPERATOR,
                new UserNameBoundary("Operator", "User"),
                "operator_avatar.png"
        );
        this.operatorUser = usersClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(OperatorNewUser)
                .retrieve()
                .body(UserBoundary.class);

        this.adminUser = usersClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(adminNewUser)
                .retrieve()
                .body(UserBoundary.class);

        // Create regular user
        NewUserBoundary regularNewUser = new NewUserBoundary(
                REGULAR_USER_EMAIL,
                UserRole.END_USER,
                new UserNameBoundary("Regular", "User"),
                "regular_avatar.png"
        );

        this.regularUser = usersClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(regularNewUser)
                .retrieve()
                .body(UserBoundary.class);
    }

    @AfterEach
    public void tearDown() {
        // Clean up after tests
        try {
            adminClient.delete().uri("/commands?userSystemID={sysId}&userEmail={email}",
                            this.adminUser.getUserId().getSystemID(), this.adminUser.getUserId().getEmail())
                    .retrieve().toBodilessEntity();
            adminClient.delete().uri("/objects?userSystemID={sysId}&userEmail={email}",
                            this.adminUser.getUserId().getSystemID(), this.adminUser.getUserId().getEmail())
                    .retrieve().toBodilessEntity();
            adminClient.delete().uri("/users?userSystemID={sysId}&userEmail={email}",
                            this.adminUser.getUserId().getSystemID(), this.adminUser.getUserId().getEmail())
                    .retrieve().toBodilessEntity();
        } catch (Exception e) {
            // Ignore cleanup failures
        }
    }

    // --- Delete All Users Tests ---
    @Test
    @DisplayName("test Delete All Users")
    public void testDeleteAllUsers() throws Exception {
        // GIVEN the server is up
        // AND the server contains users (adminUser and regularUser exist from setup)

        // Create an additional test user
        NewUserBoundary tempUser = new NewUserBoundary(
                "temp@example.com",
                UserRole.OPERATOR,
                new UserNameBoundary("Temp", "User"),
                "temp.png"
        );
        usersClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(tempUser)
                .retrieve()
                .body(UserBoundary.class);

        // WHEN I DELETE /admin/users as ADMIN
        adminClient.delete()
                .uri("/users?userSystemID={sysId}&userEmail={email}",
                        this.adminUser.getUserId().getSystemID(),
                        this.adminUser.getUserId().getEmail())
                .retrieve()
                .toBodilessEntity();

        // THEN all users are deleted
        // Verify by attempting to login as the temp user
        assertThatThrownBy(() -> usersClient.get()
                .uri("/login/{systemID}/{userEmail}", this.appName, "temp@example.com")
                .retrieve()
                .body(UserBoundary.class))
                .isInstanceOf(Exception.class)
                .extracting("statusCode.4xxClientError")
                .isEqualTo(true);
    }

    @Test
    @DisplayName("test Delete All Users Fails for Non-ADMIN")
    public void testDeleteAllUsersFailsForNonAdmin() throws Exception {
        // GIVEN a regular user exists

        // WHEN I attempt DELETE /admin/users as regular user
        // THEN the server returns 4xx status
        assertThatThrownBy(() -> adminClient.delete()
                .uri("/users?userSystemID={sysId}&userEmail={email}",
                        this.regularUser.getUserId().getSystemID(),
                        this.regularUser.getUserId().getEmail())
                .retrieve()
                .toBodilessEntity())
                .isInstanceOf(HttpClientErrorException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN);
    }

    // --- Delete All Objects Tests ---
    @Test
    @DisplayName("test Delete All Objects")
    public void testDeleteAllObjects() throws Exception {
        // GIVEN the server is up
        // AND the server contains an object
        Map<String, Object> newObjectPayload = new HashMap<>();
        newObjectPayload.put("alias", "Test Object For Deletion");
        newObjectPayload.put("type", "TEST_TYPE_DELETION");
        newObjectPayload.put("active", true);
        newObjectPayload.put("status", "testStatus");
        newObjectPayload.put("createdBy", Map.of("userId", Map.of( // Ensure this key "userId" matches ObjectBoundary's createdBy structure
                "systemID", this.operatorUser.getUserId().getSystemID(),
                "email", this.operatorUser.getUserId().getEmail())));

        // Create the object and get its details back
        @SuppressWarnings("unchecked")
        Map<String, Object> createdObjectMap = objectsClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(newObjectPayload)
                .retrieve()
                .body(Map.class); // Assuming this returns a map representation of ObjectBoundary

        assertThat(createdObjectMap).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, String> objectIdMap = (Map<String, String>) createdObjectMap.get("objectID");
        assertThat(objectIdMap).isNotNull();
        String objSystemID = objectIdMap.get("systemID");
        String objInternalId = objectIdMap.get("id");
        assertThat(objSystemID).isNotNull();
        assertThat(objInternalId).isNotNull();

        // WHEN I DELETE /admin/objects as ADMIN
        adminClient.delete()
                .uri("/objects?userSystemID={sysId}&userEmail={email}",
                        this.adminUser.getUserId().getSystemID(),
                        this.adminUser.getUserId().getEmail())
                .retrieve()
                .toBodilessEntity(); // This should return 204 No Content

        // THEN all objects are deleted
        // Verify by having the operator user attempt to fetch all objects, expecting an empty array
        ObjectBoundary[] remainingObjects = objectsClient.get()
                .uri("?userSystemID={userSysId}&userEmail={userEmail}&size={size}&page={page}",
                        this.operatorUser.getUserId().getSystemID(),
                        this.operatorUser.getUserId().getEmail(),
                        10, 0)
                .retrieve()
                .body(ObjectBoundary[].class);

        assertThat(remainingObjects).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("test Delete All Objects Fails for Non-ADMIN")
    public void testDeleteAllObjectsFailsForNonAdmin() throws Exception {
        // WHEN I attempt DELETE /admin/objects as regular user
        // THEN the server returns 403 Forbidden
        assertThatThrownBy(() -> adminClient.delete()
                .uri("/objects?userSystemID={sysId}&userEmail={email}",
                        this.regularUser.getUserId().getSystemID(),
                        this.regularUser.getUserId().getEmail())
                .retrieve()
                .toBodilessEntity())
                .isInstanceOf(HttpClientErrorException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN);
    }

    // --- Delete All Commands Tests ---
    @Test
    @DisplayName("test Delete All Commands")
    public void testDeleteAllCommands() throws Exception {
        // GIVEN the server is up
        // (Commands might exist from previous operations)

        // WHEN I DELETE /admin/commands as ADMIN
        adminClient.delete()
                .uri("/commands?userSystemID={sysId}&userEmail={email}",
                        this.adminUser.getUserId().getSystemID(),
                        this.adminUser.getUserId().getEmail())
                .retrieve()
                .toBodilessEntity();

        // THEN the database is emptied
        CommandBoundary[] actualResult = adminClient.get()
                .uri("/commands?userSystemID={sysId}&userEmail={email}&page={page}&size={size}",
                        this.adminUser.getUserId().getSystemID(),
                        this.adminUser.getUserId().getEmail(), 0, 10)
                .retrieve()
                .body(CommandBoundary[].class);

        assertThat(actualResult)
                .isNotNull()
                .isEmpty();
    }

    @Test
    @DisplayName("test Delete All Commands Fails for Non-ADMIN")
    public void testDeleteAllCommandsFailsForNonAdmin() throws Exception {
        // WHEN I attempt DELETE /admin/commands as regular user
        // THEN the server returns 403 Forbidden
        assertThatThrownBy(() -> adminClient.delete()
                .uri("/commands?userSystemID={sysId}&userEmail={email}",
                        this.regularUser.getUserId().getSystemID(),
                        this.regularUser.getUserId().getEmail())
                .retrieve()
                .toBodilessEntity())
                .isInstanceOf(HttpClientErrorException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN);
    }

    // --- Get All Users with Pagination Tests ---
    @Test
    @DisplayName("test Get All Users with Pagination")
    public void testGetAllUsersWithPagination() throws Exception {
        // GIVEN the server is up
        // AND the server contains users (adminUser and regularUser exist from setup)

        // Create additional test users to test pagination
        for (int i = 1; i <= 3; i++) {
            NewUserBoundary tempUser = new NewUserBoundary(
                    "user" + i + "@example.com",
                    UserRole.END_USER,
                    new UserNameBoundary("User" + i, "Test"),
                    "user" + i + ".png"
            );
            usersClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(tempUser)
                    .retrieve()
                    .body(UserBoundary.class);
        }

        // WHEN I GET /admin/users with pagination as ADMIN
        UserBoundary[] actualResult = adminClient.get()
                .uri("/users?userSystemID={sysId}&userEmail={email}&page={page}&size={size}",
                        this.adminUser.getUserId().getSystemID(),
                        this.adminUser.getUserId().getEmail(), 0, 10)
                .retrieve()
                .body(UserBoundary[].class);

        // THEN the server returns users with pagination
        assertThat(actualResult)
                .isNotNull()
                .hasSizeGreaterThanOrEqualTo(5); // admin + regular + 3 test users

        // Verify our known users are in the result
        assertThat(actualResult)
                .anyMatch(u -> u.getUserId().getEmail().equals(ADMIN_EMAIL))
                .anyMatch(u -> u.getUserId().getEmail().equals(REGULAR_USER_EMAIL));
    }

    @Test
    @DisplayName("test Get All Users Fails for Non-ADMIN")
    public void testGetAllUsersFailsForNonAdmin() throws Exception {
        // WHEN I attempt GET /admin/users as regular user
        // THEN the server returns 403 Forbidden
        assertThatThrownBy(() -> adminClient.get()
                .uri("/users?userSystemID={sysId}&userEmail={email}&page={page}&size={size}",
                        this.regularUser.getUserId().getSystemID(),
                        this.regularUser.getUserId().getEmail(), 0, 10)
                .retrieve()
                .body(UserBoundary[].class))
                .isInstanceOf(HttpClientErrorException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN);
    }

    // --- Get All Commands with Pagination Tests ---
    @Test
    @DisplayName("Test Get All Commands with Pagination")
    public void testGetAllCommandsWithPagination() {
        // GIVEN: Create several commands as END_USER (simulate via direct service or by invoking the endpoint)
        int numCommands = 15;
        for (int i = 0; i < numCommands; i++) {
            CommandBoundary command = new CommandBoundary();
            command.setCommand("echo");
            command.setTargetObject(Map.of("id", Map.of("objectId", "*")));
            command.setInvokedBy(Map.of("userId", this.regularUser.getUserId()));
            command.setCommandAttributes(Map.of("msg", "cmd" + i));
            List<Object> result = commandsClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(command)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Object>>() {});
        }


        // WHEN: Admin requests commands with pagination (size=5, page=1)
        CommandBoundary[] page1 = adminClient.get()
                .uri("/commands?userSystemID={sysId}&userEmail={email}&size=5&page=1",
                        adminUser.getUserId().getSystemID(),
                        adminUser.getUserId().getEmail())
                .retrieve()
                .body(CommandBoundary[].class);

        // THEN: Should get 5 commands (the second page)
        assertThat(page1).isNotNull();
        assertThat(page1.length).isEqualTo(5);

        // Optionally, check that the commands are in the expected order or have expected content
    }
    @Test
    @DisplayName("test Get All Commands Fails for Non-ADMIN")
    public void testGetAllCommandsFailsForNonAdmin() throws Exception {
        // WHEN I attempt GET /admin/commands as regular user
        // THEN the server returns 403 Forbidden
        assertThatThrownBy(() -> adminClient.get()
                .uri("/commands?userSystemID={sysId}&userEmail={email}&page={page}&size={size}",
                        this.regularUser.getUserId().getSystemID(),
                        this.regularUser.getUserId().getEmail(), 0, 10)
                .retrieve()
                .body(CommandBoundary[].class))
                .isInstanceOf(HttpClientErrorException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN);
    }
}