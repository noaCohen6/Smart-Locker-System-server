package demo;

import demo.Enums.UserRole;
import demo.PresentationLayer.Boundaries.*;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CommandControllerTests {

    private int port;
    private String appName;

    private RestClient commandsClient;
    private RestClient usersClient;
    private RestClient adminClient;
    private RestClient objectsClient;


    private final String ADMIN_CLEANUP_EMAIL = "admin.cleanup.cmd@example.com";
    private NewUserBoundary adminCleanupNewUser;
    private UserID adminCleanupUserID;

    private UserBoundary operatorUser;
    private UserBoundary end_user;
    private UserBoundary adminUser;

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
        this.commandsClient = RestClient.create("http://localhost:" + this.port + "/ambient-intelligence/commands");
        this.usersClient = RestClient.create("http://localhost:" + this.port + "/ambient-intelligence/users");
        this.adminClient = RestClient.create("http://localhost:" + this.port + "/ambient-intelligence/admin");
        this.objectsClient = RestClient.create("http://localhost:" + this.port + "/ambient-intelligence/objects");


        adminCleanupNewUser = new NewUserBoundary(ADMIN_CLEANUP_EMAIL, UserRole.ADMIN, new UserNameBoundary("AdminCmd", "CleanupCmd"), "avatar_cmd.png");

        try {
            usersClient.post().contentType(MediaType.APPLICATION_JSON).body(adminCleanupNewUser).retrieve().toBodilessEntity();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() != HttpStatus.BAD_REQUEST && e.getStatusCode() != HttpStatus.CONFLICT) {
                System.err.println("Warning: Could not create initial cleanup admin for commands, proceeding: " + e.getMessage());
            }
        }

        try {
            adminClient.delete().uri("/commands?userSystemID={sysId}&userEmail={email}", this.appName, ADMIN_CLEANUP_EMAIL).retrieve().toBodilessEntity();
        } catch (Exception e) { /* ignore */ }
        try {
            adminClient.delete().uri("/objects?userSystemID={sysId}&userEmail={email}", this.appName, ADMIN_CLEANUP_EMAIL).retrieve().toBodilessEntity();
        } catch (Exception e) { /* ignore */ }
        try {
            adminClient.delete().uri("/users?userSystemID={sysId}&userEmail={email}", this.appName, ADMIN_CLEANUP_EMAIL).retrieve().toBodilessEntity();
        } catch (Exception e) { /* ignore */ }

        UserBoundary createdAdmin = usersClient.post().contentType(MediaType.APPLICATION_JSON).body(adminCleanupNewUser).retrieve().body(UserBoundary.class);
        this.adminCleanupUserID = createdAdmin.getUserId();

        NewUserBoundary operatorNew = new NewUserBoundary("operator.cmd@example.com", UserRole.OPERATOR, new UserNameBoundary("Operator", "Cmd"), "op_cmd_avatar.png");
        this.operatorUser = usersClient.post().contentType(MediaType.APPLICATION_JSON).body(operatorNew).retrieve().body(UserBoundary.class);

        NewUserBoundary endUserNew = new NewUserBoundary("endUser.cmd@example.com", UserRole.END_USER, new UserNameBoundary("End", "UserCmd"), "end_user_cmd_avatar.png");
        this.end_user = usersClient.post().contentType(MediaType.APPLICATION_JSON).body(endUserNew).retrieve().body(UserBoundary.class);

        NewUserBoundary adminNew = new NewUserBoundary("admin.cmd@example.com", UserRole.ADMIN, new UserNameBoundary("Admin", "Cmd"), "admin_cmd_avatar.png");
        this.adminUser = usersClient.post().contentType(MediaType.APPLICATION_JSON).body(adminNew).retrieve().body(UserBoundary.class);
    }


    @AfterEach
    public void tearDown() {
        try {
            adminClient.delete().uri("/commands?userSystemID={sysId}&userEmail={email}", this.adminCleanupUserID.getSystemID(), this.adminCleanupUserID.getEmail()).retrieve().toBodilessEntity();
        } catch (Exception e) { /* ignore */ }
        try {
            adminClient.delete().uri("/objects?userSystemID={sysId}&userEmail={email}", this.adminCleanupUserID.getSystemID(), this.adminCleanupUserID.getEmail()).retrieve().toBodilessEntity();
        } catch (Exception e) { /* ignore */ }
        try {
            adminClient.delete().uri("/users?userSystemID={sysId}&userEmail={email}", this.adminCleanupUserID.getSystemID(), this.adminCleanupUserID.getEmail()).retrieve().toBodilessEntity();
        } catch (Exception e) { /* ignore */ }
    }

    // Success: END_USER can invoke ECHO command
    @Test
    @DisplayName("Invoke ECHO command as END_USER succeeds")
    public void testInvokeEchoCommandAsEndUserSucceeds() {
        CommandBoundary command = new CommandBoundary();
        command.setCommand("echo");
        command.setTargetObject(Map.of("id", Map.of("objectId", "*")));
        command.setInvokedBy(Map.of("userId", this.end_user.getUserId()));
        command.setCommandAttributes(Map.of("message", "Hello World"));

        List<Object> result = commandsClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(command)
                .retrieve()
                .body(List.class);

        assertThat(result).isNotNull();
        assertThat(result.get(0)).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) result.get(0)).get("message")).isEqualTo("Hello World");
    }

    // Fail: ADMIN cannot invoke command
    @Test
    @DisplayName("Invoke command as ADMIN fails")
    public void testInvokeCommandAsAdminFails() {
        CommandBoundary command = new CommandBoundary();
        command.setCommand("echo");
        command.setTargetObject(Map.of("id", Map.of("objectId", "*")));
        command.setInvokedBy(Map.of("userId", this.adminUser.getUserId()));
        command.setCommandAttributes(Map.of("foo", "bar"));

        assertThatThrownBy(() -> commandsClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(command)
                .retrieve()
                .body(List.class))
                .isInstanceOf(HttpClientErrorException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN);
    }

    // Fail: OPERATOR cannot invoke command
    @Test
    @DisplayName("Invoke command as OPERATOR fails")
    public void testInvokeCommandAsOperatorFails() {
        CommandBoundary command = new CommandBoundary();
        command.setCommand("echo");
        command.setTargetObject(Map.of("id", Map.of("objectId", "*")));
        command.setInvokedBy(Map.of("userId", this.operatorUser.getUserId()));
        command.setCommandAttributes(Map.of("foo", "bar"));

        assertThatThrownBy(() -> commandsClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(command)
                .retrieve()
                .body(List.class))
                .isInstanceOf(HttpClientErrorException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN);
    }

    // Fail: END_USER with missing command type (400 Bad Request)
    @Test
    @DisplayName("Invoke command as END_USER with missing type fails")
    public void testInvokeCommandAsEndUserMissingTypeFails() {
        CommandBoundary command = new CommandBoundary();
        command.setTargetObject(Map.of("id", Map.of("objectId", "*")));
        command.setInvokedBy(Map.of("userId", this.end_user.getUserId()));
        command.setCommandAttributes(Map.of("foo", "bar"));

        assertThatThrownBy(() -> commandsClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(command)
                .retrieve()
                .body(List.class))
                .isInstanceOf(HttpClientErrorException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST);
    }

    // Fail: END_USER with missing invokedBy (400 Bad Request)
    @Test
    @DisplayName("Invoke command as END_USER with missing invokedBy fails")
    public void testInvokeCommandAsEndUserMissingInvokedByFails() {
        CommandBoundary command = new CommandBoundary();
        command.setCommand("echo");
        command.setTargetObject(Map.of("id", Map.of("objectId", "*")));
        command.setCommandAttributes(Map.of("foo", "bar"));

        assertThatThrownBy(() -> commandsClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(command)
                .retrieve()
                .body(List.class))
                .isInstanceOf(HttpClientErrorException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST);
    }

    // Fail: END_USER with non-existent object (404 Not Found)
    @Test
    @DisplayName("Invoke command as END_USER with non-existent object fails")
    public void testInvokeCommandAsEndUserWithNonExistentObjectFails() {
        CommandBoundary command = new CommandBoundary();
        command.setCommand("get");
        command.setTargetObject(Map.of("id", Map.of("objectId", "nonexistent")));
        command.setInvokedBy(Map.of("userId", this.end_user.getUserId()));
        command.setCommandAttributes(Map.of());

        assertThatThrownBy(() -> commandsClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(command)
                .retrieve()
                .body(List.class))
                .isInstanceOf(HttpClientErrorException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.NOT_FOUND);
    }
}