package demo.BusinessLogicLayer.Services;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import demo.BusinessLogicLayer.Converters.ObjectConverter;
import demo.BusinessLogicLayer.Converters.UserConverter;
import demo.BusinessLogicLayer.Exceptions.MyForbiddenException;
import demo.DataAccessLayer.Entities.ObjectEntity;
import demo.DataAccessLayer.Entities.UserEntity;
import demo.Enums.UserRole;
import demo.PresentationLayer.Boundaries.CommandBoundary;
import demo.PresentationLayer.Boundaries.ObjectBoundary;
import demo.DataAccessLayer.CRUDs.CommandCrud;
import demo.BusinessLogicLayer.Converters.CommandConverter;
import demo.DataAccessLayer.Entities.CommandEntity;
import demo.BusinessLogicLayer.Exceptions.MyInvalidInputException;
import demo.BusinessLogicLayer.Exceptions.MyNotFoundException;
import demo.DataAccessLayer.IDs.UserID;
import demo.PresentationLayer.Boundaries.UserBoundary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommandServiceImpl implements CommandService {

    private final CommandCrud commandCrud;
    private final ObjectService objectService;
    private final CommandConverter converter;
    private final UserConverter userConverter;
    private final ObjectConverter objectConverter;

    private final UserService userService;
    private final UnityNotificationService unityNotificationService;
    private String appName;

    public CommandServiceImpl(
            CommandCrud commandCrud,
            ObjectService objectService, CommandConverter converter, UserService userService, ObjectConverter objectConverter, UserConverter userConverter, UnityNotificationService unityNotificationService) {
        this.commandCrud = commandCrud;
        this.objectService = objectService;
        this.converter = converter;
        this.userService = userService;
        this.objectConverter = objectConverter;
        this.userConverter = userConverter;
        this.unityNotificationService = unityNotificationService;

    }

    @Value("${spring.application.name:dummy}")
    public void setAppName(String appName) {
        this.appName = appName;
        System.err.println("*** " + this.appName);
    }

    @Override
    @Transactional
    public List<Object> invokeCommand(CommandBoundary command) {
        // Perform validation on command
        validateCommand(command);

        // Convert command to entity
        CommandEntity entity = this.converter.toEntity(command);

        //entity.setSystemID(this.appName);

        // Save the command in the database
        entity = this.commandCrud.save(entity);

        // Process the command based on its type
        String commandType = command.getCommand();
        Map<String, Object> targetObject = command.getTargetObject();
        Map<String, Object> attributes = command.getCommandAttributes();
        Map<String, UserID> invokedBy = command.getInvokedBy();

        // Execute the command and return appropriate result
        Object result = executeCommand(commandType, targetObject, attributes, invokedBy);

        // Wrap the result in a List to match the interface specification
        List<Object> resultList = new ArrayList<>();
        if (result != null) {
            if (result instanceof List) {
                resultList = (List<Object>) result;
            } else {
                resultList.add(result);
            }
        }

        return resultList;
    }

    private void validateCommand(CommandBoundary command) {
        if (command == null) {
            throw new MyInvalidInputException("Command cannot be null");
        }

        if (command.getCommand() == null || command.getCommand().trim().isEmpty()) {
            throw new MyInvalidInputException("Command type cannot be null or empty");
        }

        // Validate target object
        if (command.getTargetObject() == null) {
            throw new MyInvalidInputException("Target object cannot be null");
        }

        // Validate invokedBy
        if (command.getInvokedBy() == null) {
            throw new MyInvalidInputException("InvokedBy field cannot be null");
        }

        // Validate user ID structure
        if (command.getInvokedBy().get("userId") == null) {
            throw new MyInvalidInputException("User ID cannot be null");
        }

        UserID userId = command.getInvokedBy().get("userId");
        if (userId.getEmail() == null || userId.getEmail().trim().isEmpty()) {
            throw new MyInvalidInputException("User email cannot be null or empty");
        }

        if (userId.getSystemID() == null || userId.getSystemID().trim().isEmpty()) {
            throw new MyInvalidInputException("User system ID cannot be null or empty");
        }

        // Validate target object structure if it exists
        if (command.getTargetObject() != null && command.getTargetObject().containsKey("id")) {
            Object idObj = command.getTargetObject().get("id");
            if (!(idObj instanceof Map)) {
                throw new MyInvalidInputException("Target object ID must be a valid object");
            }
        }
    }
    private Object executeCommand(String commandType, Map<String, Object> targetObject,
                                  Map<String, Object> attributes, Map<String, UserID> invokedBy) {
        // Validate user exists and has proper permissions
        if (invokedBy == null || !invokedBy.containsKey("userId")) {
            throw new MyInvalidInputException("User information is required to execute commands");
        }

        UserID userId = invokedBy.get("userId");

        // Get user from database to check role
        UserBoundary userBoundary = userService.login(userId.getSystemID(), userId.getEmail())
                .orElseThrow(() -> new MyNotFoundException("User not found"));
        UserEntity userEntity = userConverter.toEntity(userBoundary);
        // Only END_USER can execute commands
        if (userEntity.getRole() != UserRole.END_USER) {
            throw new MyForbiddenException("Cannot execute commands");
        }

        if (targetObject == null) {
            throw new MyInvalidInputException("Target object is required");
        }

        // For all commands except possibly 'create' and 'getAvailableLockers', check if target object exists and is active
        if (!commandType.equalsIgnoreCase("create") &&
                !commandType.equalsIgnoreCase("getAvailableLockers") &&
                !isWildcardOperation(targetObject)) {
            String objectId = extractIdFromTargetObject(targetObject);
            ObjectBoundary objectBoundary = objectService.getObjectById(appName, objectId, userId.getSystemID(), userId.getEmail())
                    .orElseThrow(() -> new MyNotFoundException("Target object not found"));
            ObjectEntity objectEntity = objectConverter.toEntity(objectBoundary);
            if (!objectEntity.getActive()) {
                throw new MyNotFoundException("Target object is not active");
            }
        }

        switch (commandType.toLowerCase()) {
            case "echo":
                if (attributes == null) {
                    throw new MyInvalidInputException("Echo command requires attributes");
                }
                return attributes;

            case "create":
                if (attributes == null || attributes.isEmpty()) {
                    throw new MyInvalidInputException("Create command requires attributes with object details");
                }
                return handleCreateObject(attributes, invokedBy);

            case "update":
                return handleUpdateObject(targetObject, attributes,invokedBy);

            case "delete":
                return handleDeleteObject(targetObject,invokedBy);

            case "get":
                return handleGetObject(targetObject,invokedBy);

            case "getavailablelockers":
                return handleGetAvailableLockers(attributes, invokedBy);

            case "getreservationsbystatus":
                return handleGetReservationsByStatus(attributes, invokedBy);

            case "changelockerstatus":
                return handleChangeLockerStatus(targetObject, attributes, invokedBy);

            default:
                throw new MyInvalidInputException("Unknown command type: " + commandType);
        }
    }

    private Object handleChangeLockerStatus(Map<String, Object> targetObject, Map<String, Object> attributes, Map<String, UserID> invokedBy) {
        // Extract locker ID from the command's target object
        String lockerIdFromCommand = extractIdFromTargetObject(targetObject);
        UserID userId = invokedBy.get("userId");

        // Fetch locker object and validate
        ObjectBoundary locker = objectService.getObjectById(appName, lockerIdFromCommand, userId.getSystemID(), userId.getEmail())
                .orElseThrow(() -> new MyNotFoundException("Locker not found with ID: " + lockerIdFromCommand));
        if (!locker.getActive() || !"locker".equals(locker.getType())) {
            throw new MyForbiddenException("Access denied"); // not a valid locker object, status hidden from client
        }

        // Validate attributes contain a reservationId
        if (attributes == null || !attributes.containsKey("reservationId")) {
            throw new MyInvalidInputException("Missing 'reservationId' in command attributes."); // reservationId is required
        }
        String reservationId = attributes.get("reservationId").toString();

        // Fetch reservation object and validate
        ObjectBoundary reservation = objectService.getObjectById(appName, reservationId, userId.getSystemID(), userId.getEmail())
                .orElseThrow(() -> new MyNotFoundException("Reservation not found with ID: " + reservationId)); // Changed from MyForbiddenException to MyNotFoundException for clarity
        if (!reservation.getActive() || !"reservation".equals(reservation.getType())) {
            throw new MyForbiddenException("Access denied"); // not a valid reservation object, status hidden from client
        }

        // Check if the reservation is for the target locker by comparing lockerId in reservation's details
        Map<String, Object> reservationDetails = reservation.getObjectDetails();
        if (reservationDetails == null || !reservationDetails.containsKey("lockerId")) {
            throw new MyForbiddenException("Access denied"); // reservation details do not contain a lockerId, status hidden from client
        }
        String lockerIdFromReservation = reservationDetails.get("lockerId").toString();

        if (!lockerIdFromCommand.equals(lockerIdFromReservation)) {
            throw new MyForbiddenException("Access denied"); // The specified reservation is not for the target locker, status hidden from client
        }

        // Change locker status (isLocked) and notify Unity
        Map<String, Object> lockerDetails = locker.getObjectDetails();
        if (lockerDetails == null) {
            lockerDetails = new HashMap<>();
            locker.setObjectDetails(lockerDetails);
        }

        Boolean currentLockStatus = null;
        if (lockerDetails.containsKey("isLocked")) {
            currentLockStatus = (Boolean) lockerDetails.get("isLocked");
        }

        if (currentLockStatus == null) {
            // If isLocked is not present, it's an invalid state for toggling.
            // Based on previous logic, an explicit state was expected.
            throw new MyForbiddenException("Locker status must be 'locked' or 'unlocked'");
        } else {
            boolean newLockStatus = !currentLockStatus;
            lockerDetails.put("isLocked", newLockStatus);
            try {
                this.unityNotificationService.sendLockerStatus(lockerIdFromCommand, newLockStatus);
            } catch (Exception e) {
                // It's good practice to log this error but not fail the entire operation
                // For example: logger.error("Failed to send status update to Unity for lockerId: {}", lockerIdFromCommand, e);
                System.err.println("Failed to send status update to Unity for lockerId: " + lockerIdFromCommand);
            }
        }

        // Save the updated locker object
        objectService.updateObject(appName, lockerIdFromCommand, userId.getSystemID(), userId.getEmail(), locker, true);

        return locker; // Return the updated locker boundary
    }

    private Object handleCreateObject(Map<String, Object> attributes, Map<String, UserID> invokedBy) {
        // Convert attributes to ObjectBoundary
        ObjectBoundary objectBoundary = convertAttributesToObjectBoundary(attributes);

            // Set createdBy from invokedBy
        if (invokedBy != null && invokedBy.containsKey("userId")) {
            Map<String, UserID> createdByMap = new HashMap<>();
            createdByMap.put("userId", invokedBy.get("userId"));
            objectBoundary.setCreatedBy(createdByMap);
        }

        // Use the ObjectService to create the object
        return objectService.createObject(objectBoundary, true);
    }

    private Object handleUpdateObject(Map<String, Object> targetObject, Map<String, Object> attributes, Map<String, UserID> invokedBy) {
        // Extract ID for update operation
        String objectId = extractIdFromTargetObject(targetObject);

        // Validate attributes
        if (attributes == null || attributes.isEmpty()) {
            throw new MyInvalidInputException("Update attributes cannot be null or empty");
        }

        UserID userId = invokedBy.get("userId");

        // Check if object exists before updating
        if (!objectService.getObjectById(appName, objectId,userId.getSystemID(), userId.getEmail()).isPresent()) {
            throw new MyNotFoundException("Cannot update object with ID " + objectId + " - object not found");
        }

        // Convert attributes to ObjectBoundary
        ObjectBoundary update = convertAttributesToObjectBoundary(attributes);

        // Update the object with user authentication
        objectService.updateObject(appName, objectId, userId.getSystemID(), userId.getEmail(), update, true);
        return null; // Return null since update doesn't return a value
    }

    private Object handleDeleteObject(Map<String, Object> targetObject,Map<String, UserID> invokedBy) {
        // Check if we're deleting all objects
        if (isWildcardOperation(targetObject)) {
            objectService.deleteAllObjects(invokedBy.get("userId").getSystemID(), invokedBy.get("userId").getEmail());
            return null;
        } else {
            String objectId = extractIdFromTargetObject(targetObject);

            UserID userId = invokedBy.get("userId");

            // Check if object exists before deleting
            if (!objectService.getObjectById(appName, objectId,userId.getSystemID(), userId.getEmail()).isPresent()) {
                throw new MyNotFoundException("Cannot delete object with ID " + objectId + " - object not found");
            }

            // This would need to be implemented in ObjectService
            throw new MyInvalidInputException("Individual object deletion is not supported in this version");
        }
    }

    private Object handleGetObject(Map<String, Object> targetObject, Map<String, UserID> invokedBy) {
        // Check if we're getting all objects
        if (isWildcardOperation(targetObject)) {
            // Get user ID for authentication
            UserID userId = invokedBy.get("userId");
            if (userId == null) {
                throw new MyInvalidInputException("User ID is required to get objects");
            }

            // Use the updated getAllObjects method with pagination (default 5 items on first page)
            return objectService.getAllObjects(userId.getSystemID(), userId.getEmail(), 5, 0);
        } else {
            String objectId = extractIdFromTargetObject(targetObject);
            UserID userId = invokedBy.get("userId");
            return objectService.getObjectById(appName, objectId, userId.getSystemID(), userId.getEmail())
                    .orElseThrow(() -> new MyNotFoundException("Object with ID " + objectId + " not found"));
        }
    }

    private Object handleGetAvailableLockers(Map<String, Object> attributes, Map<String, UserID> invokedBy) {
        // Validate attributes contain required location data
        if (attributes == null) {
            throw new MyInvalidInputException("GetAvailableLockers command requires attributes with location data");
        }

        // Extract parameters from attributes
        double latitude, longitude, radius;
        int size = 20, page = 0; // Default values

        try {
            latitude = Double.parseDouble(attributes.get("latitude").toString());
            longitude = Double.parseDouble(attributes.get("longitude").toString());

            // Optional radius parameter (default 5.0 km)
            if (attributes.containsKey("radius")) {
                radius = Double.parseDouble(attributes.get("radius").toString());
            } else {
                radius = 5.0;
            }

            // Optional size parameter
            if (attributes.containsKey("size")) {
                size = Integer.parseInt(attributes.get("size").toString());
            }

            // Optional page parameter
            if (attributes.containsKey("page")) {
                page = Integer.parseInt(attributes.get("page").toString());
            }

        } catch (NumberFormatException | NullPointerException e) {
            throw new MyInvalidInputException("Invalid location parameters. Latitude and longitude are required as numbers");
        }

        UserID userId = invokedBy.get("userId");

        // Call the existing method
        return getAvailableLockersByLocation(latitude, longitude, radius,
                userId.getSystemID(), userId.getEmail(), size, page);
    }

    // Helper method to convert command attributes to ObjectBoundary
    private ObjectBoundary convertAttributesToObjectBoundary(Map<String, Object> attributes) {
        ObjectBoundary boundary = new ObjectBoundary();

        if (attributes.containsKey("type")) {
            boundary.setType(attributes.get("type").toString());
        }

        if (attributes.containsKey("alias")) {
            boundary.setAlias(attributes.get("alias").toString());
        }

        if (attributes.containsKey("active")) {
            boundary.setActive((Boolean) attributes.get("active"));
        }

        if (attributes.containsKey("status")) {
            boundary.setStatus(attributes.get("status").toString());
        }

        if (attributes.containsKey("objectDetails")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> details = (Map<String, Object>) attributes.get("objectDetails");
            boundary.setObjectDetails(details);
        }

        return boundary;
    }

    private String extractIdFromTargetObject(Map<String, Object> targetObject) {
        if (targetObject == null || !targetObject.containsKey("id")) {
            throw new MyInvalidInputException("Target object must contain an 'id' field");
        }

        Map<String, Object> idMap = (Map<String, Object>) targetObject.get("id");
        if (idMap == null || !idMap.containsKey("objectId")) {
            throw new MyInvalidInputException("Target object id must contain an 'objectId' field");
        }

        return idMap.get("objectId").toString();
    }

    private boolean isWildcardOperation(Map<String, Object> targetObject) {
        Map<String, Object> idMap = (Map<String, Object>) targetObject.get("id");
        String objectId = idMap.get("objectId").toString();
        return "*".equals(objectId) || "ALL".equalsIgnoreCase(objectId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommandBoundary> getAllCommandsHistory(String userSystemId, String userEmail, int size, int page) {
        // Verify user exists and has ADMIN role
        UserBoundary userBoundary = userService.login(userSystemId, userEmail)
                .orElseThrow(() -> new MyNotFoundException("User not found"));
        UserEntity userEntity = userConverter.toEntity(userBoundary);
        if (userEntity.getRole() != UserRole.ADMIN) {
            throw new MyForbiddenException("Only ADMIN users can retrieve command history");
        }

        // Create a Pageable object with the requested page, size, and sort by invocationTimestamp and id
        return this.commandCrud
                .findAll(PageRequest.of(page, size, Sort.Direction.ASC, "invocationTimestamp", "id"))
                .stream()
                .map(converter::toBoundary)
                .toList();
    }

    @Override
    @Transactional
    public void deleteAllCommands(String userSystemId, String userEmail) {
        // Verify user exists and has ADMIN role
        UserBoundary userBoundary = userService.login( userSystemId,  userEmail)
                .orElseThrow(() -> new MyNotFoundException("User not found"));
        UserEntity userEntity = userConverter.toEntity(userBoundary);
        if (userEntity.getRole() != UserRole.ADMIN) {
            throw new MyForbiddenException("Only ADMIN users can delete all commands");
        }

        this.commandCrud.deleteAll();
    }


    private List<Object> handleGetReservationsByStatus(
            Map<String, Object> attributes,
            Map<String, UserID> invokedBy) {

        // 1) Validate attributes
        if (attributes == null
                || !attributes.containsKey("email")
                || !attributes.containsKey("systemID")) {
            throw new MyInvalidInputException(
                    "Email and systemId are required in command attributes");
        }
        String targetEmail    = attributes.get("email").toString();
        String targetSystemID = attributes.get("systemID").toString();
        String status = attributes.containsKey("status")
                ? attributes.get("status").toString()
                : "active";

        // 2) Verify target user exists
        userService.login(targetSystemID, targetEmail)
                .orElseThrow(() -> new MyNotFoundException("Target user not found"));

        // 3) Fetch reservations of that status
        UserID caller = invokedBy.get("userId");
        List<ObjectBoundary> reservations = objectService
                .searchObjectsByTypeAndStatus(
                        "reservation",
                        status,
                        caller.getSystemID(),
                        caller.getEmail(),
                        1000,
                        0
                );

        // 4) Build simple list of reservation details
        List<Map<String, Object>> result = new ArrayList<>();
        for (ObjectBoundary r : reservations) {
            if (r.getCreatedBy() != null
                    && r.getCreatedBy().containsKey("userId")) {

                UserID creator = r.getCreatedBy().get("userId");
                if (!creator.getEmail().equals(targetEmail)
                        || !creator.getSystemID().equals(targetSystemID)) {
                    continue;
                }

                Map<String, Object> entry = new HashMap<>();
                entry.put("reservationId",     r.getObjectID().getID());
                entry.put("status",            r.getStatus());
                entry.put("creationTimestamp", r.getCreationTimestamp());

                if (r.getObjectDetails() != null) {
                    entry.putAll(r.getObjectDetails());
                }

                result.add(entry);
            }
        }

        return new ArrayList<>(result);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ObjectBoundary> getAvailableLockersByLocation(double latitude, double longitude, double radius,
                                                              String userSystemId, String userEmail, int size, int page) {
        // Validate coordinates
        if (latitude < -90 || latitude > 90) {
            throw new MyInvalidInputException("Invalid latitude value. Must be between -90 and 90");
        }

        if (longitude < -180 || longitude > 180) {
            throw new MyInvalidInputException("Invalid longitude value. Must be between -180 and 180");
        }

        if (radius <= 0) {
            throw new MyInvalidInputException("Radius must be positive");
        }

        // Verify user exists and has proper permissions
        UserBoundary userBoundary = userService.login(userSystemId, userEmail)
                .orElseThrow(() -> new MyNotFoundException("User not found"));
        UserEntity userEntity = userConverter.toEntity(userBoundary);

        // Only END_USER and OPERATOR can access lockers
        if (userEntity.getRole() == UserRole.ADMIN) {
            throw new MyForbiddenException("ADMIN users cannot access locker information");
        }

        // Get all locker blocks with type "lockerBlock" and status "available"
        List<ObjectBoundary> allAvailableLockerBlocks = objectService.searchObjectsByTypeAndStatus(
                "lockerBlock",
                "available",
                userSystemId,
                userEmail,
                1000, // Get enough to filter by distance
                0
        );

        // Filter by distance and enrich with locker children information
        List<ObjectBoundary> nearbyLockerBlocks = allAvailableLockerBlocks.stream()
                .filter(lockerBlock -> {
                    Map<String, Object> details = lockerBlock.getObjectDetails();
                    if (details != null && details.containsKey("latitude") && details.containsKey("longitude")) {
                        try {
                            double lockerLat = Double.parseDouble(details.get("latitude").toString());
                            double lockerLon = Double.parseDouble(details.get("longitude").toString());
                            double distance = calculateDistance(latitude, longitude, lockerLat, lockerLon);

                            // Add distance to object details for client use
                            details.put("distanceKm", distance);

                            // Get all locker children for this locker block
                            List<ObjectBoundary> lockerChildren = objectService.getChildren(
                                    lockerBlock.getObjectID().getSystemID(),
                                    lockerBlock.getObjectID().getID(),
                                    userSystemId,
                                    userEmail,
                                    100, // Get up to 100 lockers per block
                                    0
                            );

                            // Filter only available lockers
                            List<Map<String, Object>> availableLockers = new ArrayList<>();
                            int availableCount = 0;

                            for (ObjectBoundary locker : lockerChildren) {
                                if (locker.getType().equals("locker") &&
                                        locker.getStatus().equals("available") &&
                                        locker.getActive()) {

                                    // Create a simplified locker representation for the response
                                    Map<String, Object> lockerInfo = new HashMap<>();
                                    lockerInfo.put("id", locker.getObjectID().getID());
                                    lockerInfo.put("alias", locker.getAlias());

                                    // Add locker-specific details if they exist
                                    if (locker.getObjectDetails() != null) {
                                        if (locker.getObjectDetails().containsKey("number")) {
                                            lockerInfo.put("number", locker.getObjectDetails().get("number"));
                                        }
                                        if (locker.getObjectDetails().containsKey("size")) {
                                            lockerInfo.put("size", locker.getObjectDetails().get("size"));
                                        }
                                    }

                                    availableLockers.add(lockerInfo);
                                    availableCount++;
                                }
                            }

                            // Update the locker block with actual available lockers and count
                            details.put("availableLockers", availableLockers);
                            details.put("availableCount", availableCount);

                            return distance <= radius && availableCount > 0;
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    }
                    return false;
                })
                .sorted((a, b) -> {
                    // Sort by distance (nearest first)
                    Double distA = (Double) a.getObjectDetails().get("distanceKm");
                    Double distB = (Double) b.getObjectDetails().get("distanceKm");
                    return distA.compareTo(distB);
                })
                .collect(Collectors.toList());

        // Apply manual pagination to the filtered and sorted results
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, nearbyLockerBlocks.size());

        if (startIndex >= nearbyLockerBlocks.size()) {
            return new ArrayList<>();
        }

        return nearbyLockerBlocks.subList(startIndex, endIndex);
    }

    // Helper method to calculate distance between two coordinates
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Using Haversine formula to calculate distance in kilometers
        final double R = 6371; // Radius of the Earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // Distance in km
    }
}