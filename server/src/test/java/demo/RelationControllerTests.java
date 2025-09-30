package demo;

import demo.Enums.UserRole;
import demo.PresentationLayer.Boundaries.NewUserBoundary;
import demo.PresentationLayer.Boundaries.ObjectBoundary;
import demo.PresentationLayer.Boundaries.UserBoundary;
import demo.PresentationLayer.Boundaries.UserNameBoundary;
// ============ UPDATE 1: Added ObjectID import ============
import demo.DataAccessLayer.IDs.ObjectID;
// =========================================================
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
public class RelationControllerTests {

    private int port;
    private String appName;

    private RestClient relationClient; // Path: /objects
    private RestClient objectsClient;  // Path: /ambient-intelligence/objects (for creating objects)
    private RestClient usersClient;
    private RestClient adminClient;

    private final String ADMIN_CLEANUP_EMAIL = "admin.cleanup.rel@example.com";
    private NewUserBoundary adminCleanupNewUser;
    private UserID adminCleanupUserID;

    private UserBoundary operatorUser;
    private UserBoundary endUser;

    // ============ UPDATE 2: Added ChildIdWrapper class to match controller structure ============
    public static class ChildIdWrapper {
        private ObjectID childId;

        public ChildIdWrapper() {
        }

        public ChildIdWrapper(ObjectID childId) {
            this.childId = childId;
        }

        public ObjectID getChildId() {
            return childId;
        }

        public void setChildId(ObjectID childId) {
            this.childId = childId;
        }

        @Override
        public String toString() {
            return "ChildIdWrapper{" +
                    "childId=" + childId +
                    '}';
        }
    }
    // =============================================================================================

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
        this.relationClient = RestClient.create("http://localhost:" + this.port + "/objects"); // RelationController path
        this.objectsClient = RestClient.create("http://localhost:" + this.port + "/ambient-intelligence/objects");
        this.usersClient = RestClient.create("http://localhost:" + this.port + "/ambient-intelligence/users");
        this.adminClient = RestClient.create("http://localhost:" + this.port + "/ambient-intelligence/admin");

        adminCleanupNewUser = new NewUserBoundary(ADMIN_CLEANUP_EMAIL, UserRole.ADMIN, new UserNameBoundary("AdminRel", "CleanupRel"), "avatar_rel.png");

        try {
            usersClient.post().contentType(MediaType.APPLICATION_JSON).body(adminCleanupNewUser).retrieve().toBodilessEntity();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() != HttpStatus.BAD_REQUEST && e.getStatusCode() != HttpStatus.CONFLICT) {
                System.err.println("Warning: Could not create initial cleanup admin for relations, proceeding: " + e.getMessage());
            }
        }

        performCleanup(); // Initial cleanup attempt

        UserBoundary createdAdmin = usersClient.post().contentType(MediaType.APPLICATION_JSON).body(adminCleanupNewUser).retrieve().body(UserBoundary.class);
        this.adminCleanupUserID = createdAdmin.getUserId();

        NewUserBoundary operatorNew = new NewUserBoundary("operator.rel@example.com", UserRole.OPERATOR, new UserNameBoundary("Operator", "Rel"), "op_rel_avatar.png");
        this.operatorUser = usersClient.post().contentType(MediaType.APPLICATION_JSON).body(operatorNew).retrieve().body(UserBoundary.class);

        NewUserBoundary endUserNew = new NewUserBoundary("enduser.rel@example.com", UserRole.END_USER, new UserNameBoundary("EndUser", "Rel"), "end_rel_avatar.png");
        this.endUser = usersClient.post().contentType(MediaType.APPLICATION_JSON).body(endUserNew).retrieve().body(UserBoundary.class);
    }

    private void performCleanup() {
        try {
            adminClient.delete().uri("/commands?userSystemID={sysId}&userEmail={email}", this.appName, ADMIN_CLEANUP_EMAIL).retrieve().toBodilessEntity();
        } catch (Exception e) { /* ignore */ }
        try {
            adminClient.delete().uri("/objects?userSystemID={sysId}&userEmail={email}", this.appName, ADMIN_CLEANUP_EMAIL).retrieve().toBodilessEntity();
        } catch (Exception e) { /* ignore */ }
        try {
            adminClient.delete().uri("/users?userSystemID={sysId}&userEmail={email}", this.appName, ADMIN_CLEANUP_EMAIL).retrieve().toBodilessEntity();
        } catch (Exception e) { /* ignore */ }
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

    private ObjectBoundary createObjectViaObjectController(UserID creator, String alias, boolean active) {
        ObjectBoundary object = new ObjectBoundary();
        object.setType("RelTestType");
        object.setAlias(alias);
        object.setStatus("RelTestStatus");
        object.setActive(active);
        Map<String, UserID> createdByMap = new HashMap<>();
        createdByMap.put("userId", creator);
        object.setCreatedBy(createdByMap);
        object.setObjectDetails(new HashMap<>());
        return objectsClient.post() // Use ObjectController to create
                .contentType(MediaType.APPLICATION_JSON)
                .body(object)
                .retrieve()
                .body(ObjectBoundary.class);
    }

    @Test
    @DisplayName("Test Relate Objects Successfully by OPERATOR")
    public void testRelateObjectsSuccessfullyByOperator() {
        ObjectBoundary parentObject = createObjectViaObjectController(this.operatorUser.getUserId(), "ParentObjectRel", true);
        ObjectBoundary childObject = createObjectViaObjectController(this.operatorUser.getUserId(), "ChildObjectRel", true);

        // ============ UPDATE 3: Create wrapper instead of sending ObjectID directly ============
        ChildIdWrapper childIdWrapper = new ChildIdWrapper(childObject.getObjectID());
        // =======================================================================================

        relationClient.put()
                .uri("/{parentSystemID}/{parentObjectID}/children?userSystemID={userSysId}&userEmail={userEmail}",
                        parentObject.getObjectID().getSystemID(), parentObject.getObjectID().getID(),
                        this.operatorUser.getUserId().getSystemID(), this.operatorUser.getUserId().getEmail())
                .contentType(MediaType.APPLICATION_JSON)
                // ============ UPDATE 4: Send wrapped childId instead of direct ObjectID ============
                .body(childIdWrapper) // Changed from: .body(childObject.getObjectID())
                // ===================================================================================
                .retrieve()
                .toBodilessEntity();

        // Verify by fetching children of parent
        ObjectBoundary[] children = relationClient.get()
                .uri("/{parentSystemID}/{parentObjectID}/children?userSystemID={userSysId}&userEmail={userEmail}",
                        parentObject.getObjectID().getSystemID(), parentObject.getObjectID().getID(),
                        this.operatorUser.getUserId().getSystemID(), this.operatorUser.getUserId().getEmail())
                .retrieve()
                .body(ObjectBoundary[].class);

        assertThat(children).isNotNull().hasSize(1);
        assertThat(children[0].getObjectID().getID()).isEqualTo(childObject.getObjectID().getID());
    }

    @Test
    @DisplayName("Test Relate Objects Fails by END_USER")
    public void testRelateObjectsFailsByEndUser() {
        ObjectBoundary parentObject = createObjectViaObjectController(this.operatorUser.getUserId(), "ParentObjectRelEU", true);
        ObjectBoundary childObject = createObjectViaObjectController(this.operatorUser.getUserId(), "ChildObjectRelEU", true);

        // ============ UPDATE 5: Create wrapper instead of sending ObjectID directly ============
        ChildIdWrapper childIdWrapper = new ChildIdWrapper(childObject.getObjectID());
        // =======================================================================================

        assertThatThrownBy(() -> relationClient.put()
                .uri("/{parentSystemID}/{parentObjectID}/children?userSystemID={userSysId}&userEmail={userEmail}",
                        parentObject.getObjectID().getSystemID(), parentObject.getObjectID().getID(),
                        this.endUser.getUserId().getSystemID(), this.endUser.getUserId().getEmail())
                .contentType(MediaType.APPLICATION_JSON)
                // ============ UPDATE 6: Send wrapped childId instead of direct ObjectID ============
                .body(childIdWrapper) // Changed from: .body(childObject.getObjectID())
                // ===================================================================================
                .retrieve()
                .toBodilessEntity())
                .isInstanceOf(HttpClientErrorException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Test Get Parents Successfully by OPERATOR")
    public void testGetParentsSuccessfullyByOperator() {
        ObjectBoundary parentObject = createObjectViaObjectController(this.operatorUser.getUserId(), "ParentForGet", true);
        ObjectBoundary childObject = createObjectViaObjectController(this.operatorUser.getUserId(), "ChildForGetParent", true);

        // ============ UPDATE 7: Create wrapper instead of sending ObjectID directly ============
        ChildIdWrapper childIdWrapper = new ChildIdWrapper(childObject.getObjectID());
        // =======================================================================================

        // Relate them first
        relationClient.put()
                .uri("/{parentSystemID}/{parentObjectID}/children?userSystemID={userSysId}&userEmail={userEmail}",
                        parentObject.getObjectID().getSystemID(), parentObject.getObjectID().getID(),
                        this.operatorUser.getUserId().getSystemID(), this.operatorUser.getUserId().getEmail())
                .contentType(MediaType.APPLICATION_JSON)
                // ============ UPDATE 8: Send wrapped childId instead of direct ObjectID ============
                .body(childIdWrapper) // Changed from: .body(childObject.getObjectID())
                // ===================================================================================
                .retrieve()
                .toBodilessEntity();

        ObjectBoundary[] parents = relationClient.get()
                .uri("/{childSystemID}/{childObjectID}/parents?userSystemID={userSysId}&userEmail={userEmail}",
                        childObject.getObjectID().getSystemID(), childObject.getObjectID().getID(),
                        this.operatorUser.getUserId().getSystemID(), this.operatorUser.getUserId().getEmail())
                .retrieve()
                .body(ObjectBoundary[].class);

        assertThat(parents).isNotNull().hasSize(1);
        assertThat(parents[0].getObjectID().getID()).isEqualTo(parentObject.getObjectID().getID());
    }

    @Test
    @DisplayName("Test Get Children for END_USER (Only Active Children of Active Parent)")
    public void testGetChildrenForEndUserOnlyActive() {
        ObjectBoundary parentActive = createObjectViaObjectController(this.operatorUser.getUserId(), "ParentActiveRel", true);
        ObjectBoundary childActive = createObjectViaObjectController(this.operatorUser.getUserId(), "ChildActiveRel", true);
        ObjectBoundary childInactive = createObjectViaObjectController(this.operatorUser.getUserId(), "ChildInactiveRel", false);

        // ============ UPDATE 9: Create wrappers instead of sending ObjectIDs directly ============
        ChildIdWrapper childActiveWrapper = new ChildIdWrapper(childActive.getObjectID());
        ChildIdWrapper childInactiveWrapper = new ChildIdWrapper(childInactive.getObjectID());
        // =========================================================================================

        // Relate active child
        relationClient.put()
                .uri("/{pSysId}/{pId}/children?userSystemID={uSysId}&userEmail={uEmail}",
                        parentActive.getObjectID().getSystemID(), parentActive.getObjectID().getID(),
                        operatorUser.getUserId().getSystemID(), operatorUser.getUserId().getEmail())
                .contentType(MediaType.APPLICATION_JSON)
                // ============ UPDATE 10: Send wrapped childId instead of direct ObjectID ============
                .body(childActiveWrapper) // Changed from: .body(childActive.getObjectID())
                // ====================================================================================
                .retrieve()
                .toBodilessEntity();

        // Relate inactive child
        relationClient.put()
                .uri("/{pSysId}/{pId}/children?userSystemID={uSysId}&userEmail={uEmail}",
                        parentActive.getObjectID().getSystemID(), parentActive.getObjectID().getID(),
                        operatorUser.getUserId().getSystemID(), operatorUser.getUserId().getEmail())
                .contentType(MediaType.APPLICATION_JSON)
                // ============ UPDATE 11: Send wrapped childId instead of direct ObjectID ============
                .body(childInactiveWrapper) // Changed from: .body(childInactive.getObjectID())
                // ====================================================================================
                .retrieve()
                .toBodilessEntity();

        ObjectBoundary[] childrenForEndUser = relationClient.get()
                .uri("/{pSysId}/{pId}/children?userSystemID={uSysId}&userEmail={uEmail}",
                        parentActive.getObjectID().getSystemID(), parentActive.getObjectID().getID(),
                        endUser.getUserId().getSystemID(), endUser.getUserId().getEmail())
                .retrieve()
                .body(ObjectBoundary[].class);

        assertThat(childrenForEndUser).isNotNull().hasSize(1);
        assertThat(childrenForEndUser[0].getObjectID().getID()).isEqualTo(childActive.getObjectID().getID());
        assertThat(childrenForEndUser[0].getActive()).isTrue();
    }
}