package demo;

import demo.Enums.UserRole;
import demo.PresentationLayer.Boundaries.NewUserBoundary;
import demo.PresentationLayer.Boundaries.ObjectBoundary;
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

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ObjectControllerTests {

    private int port;
    private String appName;

    private RestClient objectsClient;
    private RestClient usersClient; // To create users
    private RestClient adminClient; // For cleanup

    private final String ADMIN_CLEANUP_EMAIL = "admin.cleanup.obj@example.com";
    private NewUserBoundary adminCleanupNewUser;
    private UserID adminCleanupUserID;

    private UserBoundary operatorUser;
    private UserBoundary endUser;
    private UserBoundary adminUser; // Regular admin, not cleanup admin

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
        this.objectsClient = RestClient.create("http://localhost:" + this.port + "/ambient-intelligence/objects");
        this.usersClient = RestClient.create("http://localhost:" + this.port + "/ambient-intelligence/users");
        this.adminClient = RestClient.create("http://localhost:" + this.port + "/ambient-intelligence/admin");

        adminCleanupNewUser = new NewUserBoundary();
        adminCleanupNewUser.setEmail(ADMIN_CLEANUP_EMAIL);
        adminCleanupNewUser.setRole(UserRole.ADMIN);
        adminCleanupNewUser.setUsername(new UserNameBoundary("AdminObj", "CleanupObj"));
        adminCleanupNewUser.setAvatar("avatar_cleanup_obj.png");

        try {
            usersClient.post().contentType(MediaType.APPLICATION_JSON).body(adminCleanupNewUser).retrieve().toBodilessEntity();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() != HttpStatus.BAD_REQUEST && e.getStatusCode() != HttpStatus.CONFLICT) {
                 System.err.println("Warning: Could not create initial cleanup admin for objects, proceeding: " + e.getMessage());
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

        // Create OPERATOR user
        NewUserBoundary operatorNew = new NewUserBoundary("operator.obj@example.com", UserRole.OPERATOR, new UserNameBoundary("Operator", "Obj"), "op_avatar.png");
        this.operatorUser = usersClient.post().contentType(MediaType.APPLICATION_JSON).body(operatorNew).retrieve().body(UserBoundary.class);

        // Create END_USER user
        NewUserBoundary endUserNew = new NewUserBoundary("enduser.obj@example.com", UserRole.END_USER, new UserNameBoundary("EndUser", "Obj"), "end_avatar.png");
        this.endUser = usersClient.post().contentType(MediaType.APPLICATION_JSON).body(endUserNew).retrieve().body(UserBoundary.class);
        
        // Create regular ADMIN user (distinct from cleanup admin)
        NewUserBoundary adminNew = new NewUserBoundary("admin.obj@example.com", UserRole.ADMIN, new UserNameBoundary("Admin", "ObjTest"), "admin_obj_avatar.png");
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

    private ObjectBoundary createSampleObjectBoundary(UserID creatorId, boolean active) {
        ObjectBoundary object = new ObjectBoundary();
        object.setType("TestType");
        object.setAlias("TestAlias");
        object.setStatus("TestStatus");
        object.setActive(active);
        Map<String, UserID> createdByMap = new HashMap<>();
        createdByMap.put("userId", creatorId);
        object.setCreatedBy(createdByMap);
        object.setObjectDetails(new HashMap<>());
        return object;
    }

    @Test
    @DisplayName("Test Create Object Successfully by OPERATOR")
    public void testCreateObjectSuccessfullyByOperator() {
        ObjectBoundary newObject = createSampleObjectBoundary(this.operatorUser.getUserId(), true);

        ObjectBoundary createdObject = objectsClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(newObject)
                .retrieve()
                .body(ObjectBoundary.class);

        assertThat(createdObject).isNotNull();
        assertThat(createdObject.getObjectID().getSystemID()).isEqualTo(this.appName);
        assertThat(createdObject.getAlias()).isEqualTo(newObject.getAlias());
        assertThat(createdObject.getActive()).isTrue();
        assertThat(createdObject.getCreatedBy().get("userId").getEmail()).isEqualTo(this.operatorUser.getUserId().getEmail());
    }

    @Test
    @DisplayName("Test Create Object Fails by END_USER")
    public void testCreateObjectFailsByEndUser() {
        ObjectBoundary newObject = createSampleObjectBoundary(this.endUser.getUserId(), true);

        assertThatThrownBy(() -> objectsClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(newObject)
                .retrieve()
                .body(ObjectBoundary.class))
                .isInstanceOf(HttpClientErrorException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN);
    }
    
    @Test
    @DisplayName("Test Create Object Fails With Missing Type")
    public void testCreateObjectFailsWithMissingType() {
        ObjectBoundary newObject = createSampleObjectBoundary(this.operatorUser.getUserId(), true);
        newObject.setType(null); // Missing type

        assertThatThrownBy(() -> objectsClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(newObject)
                .retrieve()
                .body(ObjectBoundary.class))
                .isInstanceOf(HttpClientErrorException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST); // Or specific error for invalid input
    }


    @Test
    @DisplayName("Test Get All Objects by OPERATOR")
    public void testGetAllObjectsByOperator() {
        // GIVEN some objects exist
        objectsClient.post().contentType(MediaType.APPLICATION_JSON).body(createSampleObjectBoundary(this.operatorUser.getUserId(), true)).retrieve().body(ObjectBoundary.class);
        objectsClient.post().contentType(MediaType.APPLICATION_JSON).body(createSampleObjectBoundary(this.operatorUser.getUserId(), false)).retrieve().body(ObjectBoundary.class);

        ObjectBoundary[] allObjects = objectsClient.get()
                .uri("?userSystemID={sysId}&userEmail={email}&size=10&page=0",
                        this.operatorUser.getUserId().getSystemID(), this.operatorUser.getUserId().getEmail())
                .retrieve()
                .body(ObjectBoundary[].class);

        assertThat(allObjects).isNotNull();
        assertThat(allObjects.length).isGreaterThanOrEqualTo(2); // Includes objects created by operator
    }

    @Test
    @DisplayName("Test Get All Objects by END_USER (Only Active)")
    public void testGetAllObjectsByEndUserOnlyActive() {
        objectsClient.post().contentType(MediaType.APPLICATION_JSON).body(createSampleObjectBoundary(this.operatorUser.getUserId(), true)).retrieve().body(ObjectBoundary.class);
        objectsClient.post().contentType(MediaType.APPLICATION_JSON).body(createSampleObjectBoundary(this.operatorUser.getUserId(), false)).retrieve().body(ObjectBoundary.class);

        ObjectBoundary[] activeObjects = objectsClient.get()
                .uri("?userSystemID={sysId}&userEmail={email}&size=10&page=0",
                        this.endUser.getUserId().getSystemID(), this.endUser.getUserId().getEmail())
                .retrieve()
                .body(ObjectBoundary[].class);

        assertThat(activeObjects).isNotNull();
        assertThat(activeObjects).allMatch(ObjectBoundary::getActive);
        // Check count based on active objects created. If only one active, length should be 1.
        long expectedActiveCount = 1; // Assuming one active, one inactive created above
        assertThat(activeObjects.length).isEqualTo(expectedActiveCount);
    }
    
    @Test
    @DisplayName("Test Get All Objects by ADMIN Fails")
    public void testGetAllObjectsByAdminFails() {
         assertThatThrownBy(() -> objectsClient.get()
                .uri("?userSystemID={sysId}&userEmail={email}&size=10&page=0",
                        this.adminUser.getUserId().getSystemID(), this.adminUser.getUserId().getEmail())
                .retrieve()
                .body(ObjectBoundary[].class))
                .isInstanceOf(HttpClientErrorException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN);
    }


    @Test
    @DisplayName("Test Get Single Object by OPERATOR")
    public void testGetSingleObjectByOperator() {
        ObjectBoundary created = objectsClient.post().contentType(MediaType.APPLICATION_JSON).body(createSampleObjectBoundary(this.operatorUser.getUserId(), true)).retrieve().body(ObjectBoundary.class);

        ObjectBoundary fetched = objectsClient.get()
                .uri("/{systemID}/{objectID}?userSystemID={userSysId}&userEmail={userEmail}",
                        created.getObjectID().getSystemID(), created.getObjectID().getID(),
                        this.operatorUser.getUserId().getSystemID(), this.operatorUser.getUserId().getEmail())
                .retrieve()
                .body(ObjectBoundary.class);

        assertThat(fetched).isNotNull();
        assertThat(fetched.getObjectID().getID()).isEqualTo(created.getObjectID().getID());
    }
    
    @Test
    @DisplayName("Test Get Single Inactive Object by END_USER Fails")
    public void testGetSingleInactiveObjectByEndUserFails() {
        ObjectBoundary createdInactive = objectsClient.post().contentType(MediaType.APPLICATION_JSON).body(createSampleObjectBoundary(this.operatorUser.getUserId(), false)).retrieve().body(ObjectBoundary.class);

        assertThatThrownBy(() -> objectsClient.get()
                .uri("/{systemID}/{objectID}?userSystemID={userSysId}&userEmail={userEmail}",
                        createdInactive.getObjectID().getSystemID(), createdInactive.getObjectID().getID(),
                        this.endUser.getUserId().getSystemID(), this.endUser.getUserId().getEmail())
                .retrieve()
                .body(ObjectBoundary.class))
                .isInstanceOf(HttpClientErrorException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN); // As per ObjectServiceImpl logic
    }


    @Test
    @DisplayName("Test Update Object Successfully by OPERATOR")
    public void testUpdateObjectSuccessfullyByOperator() {
        ObjectBoundary originalObject = objectsClient.post().contentType(MediaType.APPLICATION_JSON).body(createSampleObjectBoundary(this.operatorUser.getUserId(), true)).retrieve().body(ObjectBoundary.class);

        ObjectBoundary updatePayload = new ObjectBoundary();
        updatePayload.setAlias("UpdatedAlias");
        updatePayload.setType("UpdatedType"); // Type must be non-empty
        updatePayload.setStatus("UpdatedStatus"); // Status must be non-empty
        updatePayload.setActive(false);

        objectsClient.put()
                .uri("/{systemID}/{objectID}?userSystemID={userSysId}&userEmail={userEmail}",
                        originalObject.getObjectID().getSystemID(), originalObject.getObjectID().getID(),
                        this.operatorUser.getUserId().getSystemID(), this.operatorUser.getUserId().getEmail())
                .contentType(MediaType.APPLICATION_JSON)
                .body(updatePayload)
                .retrieve()
                .toBodilessEntity();

        ObjectBoundary updatedObject = objectsClient.get()
                 .uri("/{systemID}/{objectID}?userSystemID={userSysId}&userEmail={userEmail}",
                        originalObject.getObjectID().getSystemID(), originalObject.getObjectID().getID(),
                        this.operatorUser.getUserId().getSystemID(), this.operatorUser.getUserId().getEmail())
                .retrieve()
                .body(ObjectBoundary.class);

        assertThat(updatedObject.getAlias()).isEqualTo("UpdatedAlias");
        assertThat(updatedObject.getActive()).isFalse();
        assertThat(updatedObject.getType()).isEqualTo("UpdatedType");
        assertThat(updatedObject.getStatus()).isEqualTo("UpdatedStatus");
    }
    
    @Test
    @DisplayName("Test Update Object Fails by END_USER")
    public void testUpdateObjectFailsByEndUser() {
        ObjectBoundary originalObject = objectsClient.post().contentType(MediaType.APPLICATION_JSON).body(createSampleObjectBoundary(this.operatorUser.getUserId(), true)).retrieve().body(ObjectBoundary.class);

        ObjectBoundary updatePayload = new ObjectBoundary();
        updatePayload.setAlias("AttemptedUpdateAlias");
        updatePayload.setType("UnchangedType"); 
        updatePayload.setStatus("UnchangedStatus");


        assertThatThrownBy(() -> objectsClient.put()
                .uri("/{systemID}/{objectID}?userSystemID={userSysId}&userEmail={userEmail}",
                        originalObject.getObjectID().getSystemID(), originalObject.getObjectID().getID(),
                        this.endUser.getUserId().getSystemID(), this.endUser.getUserId().getEmail())
                .contentType(MediaType.APPLICATION_JSON)
                .body(updatePayload)
                .retrieve()
                .toBodilessEntity())
                .isInstanceOf(HttpClientErrorException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN);
    }
}