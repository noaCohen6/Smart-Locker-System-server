package demo.Initializers;

      import demo.BusinessLogicLayer.Services.CommandService;
      import demo.BusinessLogicLayer.Services.ObjectService;
      import demo.BusinessLogicLayer.Services.UserService;
      import demo.DataAccessLayer.IDs.UserID;
      import demo.Enums.UserRole;
      import demo.PresentationLayer.Boundaries.NewUserBoundary;
      import demo.PresentationLayer.Boundaries.ObjectBoundary;
      import demo.PresentationLayer.Boundaries.UserBoundary;
      import demo.PresentationLayer.Boundaries.UserNameBoundary;
      import demo.BusinessLogicLayer.Exceptions.MyForbiddenException;
      import demo.BusinessLogicLayer.Exceptions.MyInvalidInputException;

      import org.apache.commons.logging.Log;
      import org.apache.commons.logging.LogFactory;
      import org.springframework.beans.factory.annotation.Autowired;
      import org.springframework.beans.factory.annotation.Value;
      import org.springframework.boot.CommandLineRunner;
      import org.springframework.context.annotation.Profile;
      import org.springframework.stereotype.Component;
      import java.util.ArrayList;
      import java.util.HashMap;
      import java.util.List;
      import java.util.Map;
      import java.util.Random;
      import java.util.concurrent.ThreadLocalRandom;

      @Component
      @Profile("LockerManualGeneration")
      public class MyInitHelper implements CommandLineRunner {

          private final Log log = LogFactory.getLog(MyInitHelper.class);
          private final UserService userService;
          private final ObjectService objectService;
          private final CommandService commandService;
          private final Random random = new Random();

          @Value("${spring.application.name:dummy}")
          private String appName;

          private static final String[] LOCKER_SIZES = {"small", "medium", "large"};
          private static final String[] LOCKER_BLOCK_NAMES_PREFIX = {"Alpha", "Beta", "Gamma", "Delta", "Epsilon"};
          private static final String[] LOCKER_BLOCK_NAMES_SUFFIX = {"Tower", "Plaza", "Point", "Central", "Junction"};
          private static final String[] STREET_NAMES = {"Main St", "Oak Ave", "Pine Ln", "Maple Dr", "Cedar Rd"};

          @Autowired
          public MyInitHelper(UserService userService, ObjectService objectService, CommandService commandService) {
              this.userService = userService;
              this.objectService = objectService;
              this.commandService = commandService;
          }

          @Override
          public void run(String... args) throws Exception {
              log.info("Starting data generation for profile 'LockerManualGeneration'...");

              UserBoundary bootstrapAdmin = getOrCreateBootstrapAdmin();
              log.info("Using bootstrap admin: " + bootstrapAdmin.getUserId().getEmail());

              clearAllData(bootstrapAdmin);
              // Re-ensure bootstrap admin exists if it was deleted by deleteAllUsers
              bootstrapAdmin = getOrCreateBootstrapAdmin();

              Map<UserRole, List<UserBoundary>> createdUsers = initUsers();
              UserBoundary operatorUser = createdUsers.get(UserRole.OPERATOR).get(0); // Assuming one operator
              List<UserBoundary> endUsers = createdUsers.get(UserRole.END_USER);

              List<ObjectBoundary> allCreatedLockers = initLockerInfrastructure(operatorUser);

              log.info("Data generation completed.");
          }

          private UserBoundary getOrCreateBootstrapAdmin() {
              String bootstrapEmail = "bootstrap@afekalocker.com";
              try {
                  return userService.login(appName, bootstrapEmail)
                          .orElseGet(() -> {
                              log.info("Bootstrap admin not found, creating one...");
                              return createBootstrapAdmin(bootstrapEmail);
                          });
              } catch (MyForbiddenException | MyInvalidInputException e) {
                  log.warn("Login failed for bootstrap admin (might be due to not existing): " + e.getMessage() + ". Attempting to create.");
                  return createBootstrapAdmin(bootstrapEmail);
              } catch (Exception e) {
                  log.error("Unexpected error during getOrCreateBootstrapAdmin: " + e.getMessage() + ". Attempting to create.");
                  return createBootstrapAdmin(bootstrapEmail);
              }
          }

          private UserBoundary createBootstrapAdmin(String email) {
              try {
                  return userService.createUser(new NewUserBoundary(email,  UserRole.ADMIN ,new UserNameBoundary("Bootstrap", "Admin"), "bootstrap_avatar.png"));
              } catch (MyInvalidInputException e) {
                  log.warn("Failed to create bootstrap admin, possibly already exists now: " + e.getMessage() + ". Attempting login again.");
                  return userService.login(appName, email)
                                    .orElseThrow(() -> new RuntimeException("Failed to get or create bootstrap admin user after multiple attempts: " + email));
              }
          }

          private void clearAllData(UserBoundary adminUser) {
              log.info("Attempting to delete all commands...");
              try {
                  commandService.deleteAllCommands(adminUser.getUserId().getSystemID(), adminUser.getUserId().getEmail());
                  log.info("All commands deleted.");
              } catch (Exception e) {
                  log.warn("Could not delete all commands: " + e.getMessage());
              }

              log.info("Attempting to delete all objects...");
              try {
                  objectService.deleteAllObjects(adminUser.getUserId().getSystemID(), adminUser.getUserId().getEmail());
                  log.info("All objects deleted.");
              } catch (Exception e) {
                  log.warn("Could not delete all objects: " + e.getMessage());
              }

              log.info("Attempting to delete all users...");
              try {
                  userService.deleteAllUsers(adminUser.getUserId().getSystemID(), adminUser.getUserId().getEmail());
                  log.info("All users deleted.");
              } catch (Exception e) {
                  log.warn("Could not delete all users: " + e.getMessage());
              }
          }

          private Map<UserRole, List<UserBoundary>> initUsers() {
              Map<UserRole, List<UserBoundary>> usersMap = new HashMap<>();
              usersMap.put(UserRole.ADMIN, new ArrayList<>());
              usersMap.put(UserRole.OPERATOR, new ArrayList<>());
              usersMap.put(UserRole.END_USER, new ArrayList<>());

              UserBoundary appAdminUser = userService.createUser(new NewUserBoundary("admin@example.com", UserRole.ADMIN, new UserNameBoundary("App", "Admin"), "admin_avatar.png"));
              log.info("Created Admin User: " + appAdminUser.getUserId().getEmail());
              usersMap.get(UserRole.ADMIN).add(appAdminUser);

              UserBoundary operatorUser = userService.createUser(new NewUserBoundary("operator@example.com", UserRole.OPERATOR, new UserNameBoundary("App", "Operator"), "operator_avatar.png"));
              log.info("Created Operator User: " + operatorUser.getUserId().getEmail());
              usersMap.get(UserRole.OPERATOR).add(operatorUser);

              usersMap.get(UserRole.END_USER).add(userService.createUser(new NewUserBoundary("enduser@example.com", UserRole.END_USER, new UserNameBoundary("Alice", "Smith"), "avatar_alice.png")));
              usersMap.get(UserRole.END_USER).add(userService.createUser(new NewUserBoundary("bob.johnson@example.com", UserRole.END_USER, new UserNameBoundary("Bob", "Johnson"), "avatar_bob.png")));
              usersMap.get(UserRole.END_USER).add(userService.createUser(new NewUserBoundary("charlie.brown@example.com", UserRole.END_USER, new UserNameBoundary("Charlie", "Brown"), "avatar_charlie.png")));
              usersMap.get(UserRole.END_USER).forEach(u -> log.info("Created End User: " + u.getUserId().getEmail()));

              return usersMap;
          }

          private List<ObjectBoundary> initLockerInfrastructure(UserBoundary operatorUser) {
              List<ObjectBoundary> allCreatedLockers = new ArrayList<>();
              Map<String, UserID> createdByOperator = new HashMap<>();
              createdByOperator.put("userId", operatorUser.getUserId());

              for (int i = 0; i < 3; i++) { // Create 3 LockerBlocks
                  String blockName = LOCKER_BLOCK_NAMES_PREFIX[random.nextInt(LOCKER_BLOCK_NAMES_PREFIX.length)] + " " +
                                     LOCKER_BLOCK_NAMES_SUFFIX[random.nextInt(LOCKER_BLOCK_NAMES_SUFFIX.length)] + " " + (i + 1);
                  String address = (100 + random.nextInt(899)) + " " + STREET_NAMES[random.nextInt(STREET_NAMES.length)];
                  double latitude = ThreadLocalRandom.current().nextDouble(32.05, 32.15);
                  double longitude = ThreadLocalRandom.current().nextDouble(34.75, 34.85);

                  ObjectBoundary lockerBlockBoundary = new ObjectBoundary();
                  lockerBlockBoundary.setType("lockerBlock");
                  lockerBlockBoundary.setAlias(blockName);
                  lockerBlockBoundary.setActive(true);
                  lockerBlockBoundary.setStatus("available");
                  lockerBlockBoundary.setCreatedBy(createdByOperator);

                  Map<String, Object> blockDetails = new HashMap<>();
                  blockDetails.put("name", blockName);
                  blockDetails.put("address", address);
                  blockDetails.put("latitude", latitude);
                  blockDetails.put("longitude", longitude);
                  lockerBlockBoundary.setObjectDetails(blockDetails);

                  ObjectBoundary createdLockerBlock = objectService.createObject(lockerBlockBoundary, false);
                  log.info("Created LockerBlock: ID=" + createdLockerBlock.getObjectID().getID() + ", Name=" + blockName);

                  int numLockersInBlock = 5 + random.nextInt(6); // 5 to 10 lockers
                  int availableCountInBlock = 0;

                  for (int j = 0; j < numLockersInBlock; j++) {
                      int lockerNumber = (i * 100) + j + 1;
                      String lockerSize = LOCKER_SIZES[random.nextInt(LOCKER_SIZES.length)];
                      String[] possibleStatuses = {"available", "available", "available", "OutOfOrder"};
                      String lockerStatus = possibleStatuses[random.nextInt(possibleStatuses.length)];

                      if ("available".equals(lockerStatus)) {
                          availableCountInBlock++;
                      }

                      ObjectBoundary lockerBoundary = new ObjectBoundary();
                      lockerBoundary.setType("locker");
                      lockerBoundary.setAlias("Locker " + lockerNumber);
                      lockerBoundary.setActive(true);
                      lockerBoundary.setStatus(lockerStatus);
                      lockerBoundary.setCreatedBy(createdByOperator);

                      Map<String, Object> lockerDetails = new HashMap<>();
                      lockerDetails.put("number", lockerNumber);
                      lockerDetails.put("size", lockerSize);
                      lockerDetails.put("isLocked", false);
                      lockerBoundary.setObjectDetails(lockerDetails);

                      ObjectBoundary createdLocker = objectService.createObject(lockerBoundary, false);
                      log.info("  Created Locker: ID=" + createdLocker.getObjectID().getID() + ", Number=" + lockerNumber + ", Status=" + lockerStatus);
                      allCreatedLockers.add(createdLocker);

                      objectService.bindObjects(
                              appName, createdLockerBlock.getObjectID().getID(),
                              appName, createdLocker.getObjectID().getID(),
                              operatorUser.getUserId().getSystemID(), operatorUser.getUserId().getEmail()
                      );
                      log.info("  Bound Locker ID=" + createdLocker.getObjectID().getID() + " to LockerBlock ID=" + createdLockerBlock.getObjectID().getID());
                  }

                  createdLockerBlock.getObjectDetails().put("availableCount", availableCountInBlock);
                  ObjectBoundary updateBlockPayload = new ObjectBoundary();
                  updateBlockPayload.setType(createdLockerBlock.getType());
                  updateBlockPayload.setAlias(createdLockerBlock.getAlias());
                  updateBlockPayload.setStatus(createdLockerBlock.getStatus());
                  updateBlockPayload.setObjectDetails(createdLockerBlock.getObjectDetails());
                  updateBlockPayload.setActive(createdLockerBlock.getActive());

                  objectService.updateObject(appName, createdLockerBlock.getObjectID().getID(),
                                             operatorUser.getUserId().getSystemID(), operatorUser.getUserId().getEmail(),
                                             updateBlockPayload, false);
                  log.info("Updated LockerBlock ID=" + createdLockerBlock.getObjectID().getID() + " with availableCount=" + availableCountInBlock);
              }
              return allCreatedLockers;
          }
      }