package demo.BusinessLogicLayer.Services;

import demo.DataAccessLayer.IDs.UserID;
import demo.PresentationLayer.Boundaries.ObjectBoundary;
import demo.PresentationLayer.Boundaries.UserBoundary;
import demo.DataAccessLayer.CRUDs.ObjectCrud;
import demo.DataAccessLayer.Entities.ObjectEntity;
import demo.DataAccessLayer.Entities.UserEntity;
import demo.BusinessLogicLayer.Converters.ObjectConverter;
import demo.BusinessLogicLayer.Converters.UserConverter;
import demo.BusinessLogicLayer.Exceptions.MyForbiddenException;
import demo.BusinessLogicLayer.Exceptions.MyInvalidInputException;
import demo.BusinessLogicLayer.Exceptions.MyNotFoundException;
import demo.Enums.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class ObjectServiceImpl implements ObjectService {

    private final ObjectCrud objectCrud;
    private final ObjectConverter converter;
    private final UserService userService;
    private final UserConverter userConverter;
    private String appName;

    @Autowired
    public ObjectServiceImpl(
            ObjectCrud objectCrud,
            ObjectConverter converter,
            UserService userService,
            UserConverter userConverter) {
        this.objectCrud = objectCrud;
        this.converter = converter;
        this.userService = userService;
        this.userConverter = userConverter;
    }

    @Value("${spring.application.name:dummy}")
    public void setAppName(String appName) {
        this.appName = appName;
        System.err.println("*** " + this.appName);
    }

    @Override
    @Transactional
    public ObjectBoundary createObject(ObjectBoundary object,Boolean fromCommand) {
        if (object.getType() == null) {
            throw new MyInvalidInputException("Object type cannot be null");
        }
        if (object.getType().trim().isEmpty()) {
            throw new MyInvalidInputException("Object type cannot be empty");
        }

        if (object.getAlias() == null) {
            throw new MyInvalidInputException("Object alias cannot be null");
        }
        if (object.getAlias().trim().isEmpty()) {
            throw new MyInvalidInputException("Object alias cannot be empty");
        }

        if (object.getStatus() == null || object.getStatus().trim().isEmpty()) {
            throw new MyInvalidInputException("Object status cannot be null or empty");
        }

        // Validate createdBy field - must be present and user must exist
        if (object.getCreatedBy() == null) {
            throw new MyInvalidInputException("CreatedBy field cannot be null");
        }

        if (!object.getCreatedBy().containsKey("userId")) {
            throw new MyInvalidInputException("CreatedBy must contain userId field");
        }

        UserID userId = object.getCreatedBy().get("userId");
        if (userId == null) {
            throw new MyInvalidInputException("UserId cannot be null");
        }

        if (userId.getEmail() == null || userId.getEmail().trim().isEmpty()) {
            throw new MyInvalidInputException("Creator email cannot be null or empty");
        }

        // Check if the user exists in the system and get their role
        UserBoundary userBoundary = userService.login(userId.getSystemID(), userId.getEmail())
                .orElseThrow(() -> new MyNotFoundException("User with email " + userId.getEmail() + " does not exist"));
        UserEntity userEntity = userConverter.toEntity(userBoundary);

        if (userEntity.getRole()!= UserRole.OPERATOR &&  fromCommand==false)
            throw new MyForbiddenException("Cannot create object");

        ObjectEntity entity = this.converter.toEntity(object);
        entity.setId(UUID.randomUUID().toString());
        entity.setSystemID(this.appName);

        entity.setCreationTimestamp(new Date());

        if (object.getCreatedBy() != null && object.getCreatedBy().containsKey("userId")) {
            UserID userIdFromMap = object.getCreatedBy().get("userId");
            String userStr = userIdFromMap.getEmail() + "/" + userIdFromMap.getSystemID();
            entity.setCreatedBy(userStr);
        }

        return converter.toBoundary(this.objectCrud.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ObjectBoundary> getAllObjects(String userSystemID, String userEmail, int size, int page) {
        // Verify user exists and get their role
        UserBoundary userBoundary = userService.login(userSystemID, userEmail)
                .orElseThrow(() -> new MyNotFoundException("User not found"));
        UserEntity userEntity = userConverter.toEntity(userBoundary);

        // Apply permission rules based on user role
        switch (userEntity.getRole()) {
            case ADMIN:
                // ADMIN users have no access
                throw new MyForbiddenException("Objects not found");

            case OPERATOR:
                // OPERATOR users have full access to all objects with pagination
                return this.objectCrud
                        .findAll(PageRequest.of(page, size, Sort.Direction.ASC, "creationTimestamp", "id"))
                        .stream()
                        .map(converter::toBoundary)
                        .collect(Collectors.toList());

            case END_USER:
                // END_USER can only access active objects with pagination
                return this.objectCrud
                        .findAll(PageRequest.of(page, size, Sort.Direction.ASC, "creationTimestamp", "id"))
                        .stream()
                        .filter(entity -> entity.getActive() != null && entity.getActive())
                        .map(converter::toBoundary)
                        .collect(Collectors.toList());

            default:
                throw new MyForbiddenException("Unknown user role");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ObjectBoundary> getObjectById(String systemID, String objectId, String userSystemID, String userEmail) {
        // First, get the object entity
        ObjectEntity entity = objectCrud.findById(objectId)
                .orElseThrow(() -> new MyNotFoundException("Object with ID " + objectId + " not found"));

        // Verify user exists and get their role
        UserBoundary userBoundary = userService.login(userSystemID, userEmail)
                .orElseThrow(() -> new MyNotFoundException("User not found"));
        UserEntity userEntity = userConverter.toEntity(userBoundary);

        // Apply permission rules based on user role
        switch (userEntity.getRole()) {
            case ADMIN:
                // ADMIN users have no access
                throw new MyForbiddenException("Object not found");

            case OPERATOR:
                // OPERATOR users have full access
                break;

            case END_USER:
                // END_USER can only access active objects
                if (entity.getActive() == null || !entity.getActive()) {
                    throw new MyForbiddenException("Object not found");
                }
                break;

            default:
                throw new MyForbiddenException("Unknown user role");
        }

        // Convert the entity to a boundary object and return it
        return Optional.of(converter.toBoundary(entity));
    }

    @Override
    @Transactional
    public void updateObject(String systemID, String objectId, String userSystemID, String userEmail, ObjectBoundary update,Boolean fromCommand) {
        // Verify user exists
        UserBoundary userBoundary = userService.login(userSystemID, userEmail)
                .orElseThrow(() -> new MyNotFoundException("User not found"));
        UserEntity userEntity = userConverter.toEntity(userBoundary);

        // Get the object entity
        ObjectEntity existingEntity = objectCrud.findById(objectId)
                .orElseThrow(() -> new MyNotFoundException("Object with ID " + objectId + " not found"));

        if (userEntity.getRole()!= UserRole.OPERATOR && fromCommand==false)
            throw new MyForbiddenException("Cannot update object");

        // Validate type if provided
        if (update.getType()==null || update.getType().trim().isEmpty()) {
            throw new MyInvalidInputException("Object type cannot be empty or null");
        }
        existingEntity.setType(update.getType());

        // Validate alias if provided
        if (update.getAlias()==null || update.getAlias().trim().isEmpty()) {
            throw new MyInvalidInputException("Object alias cannot be empty or null");
        }
        existingEntity.setAlias(update.getAlias());

        if (update.getStatus() == null || update.getStatus().trim().isEmpty()) {
            throw new MyInvalidInputException("Object status cannot be null or empty");
        }
        existingEntity.setStatus(update.getStatus());

        // Update fields
        if (update.getActive() != null) existingEntity.setActive(update.getActive());
        if (update.getObjectDetails() != null) {
            existingEntity.setObjectDetails(update.getObjectDetails());
        }

        objectCrud.save(existingEntity);
    }

    @Override
    @Transactional
    public void deleteAllObjects(String userSystemId, String userEmail) {
        // Verify user exists and has ADMIN role
        UserBoundary userBoundary = userService.login(userSystemId, userEmail)
                .orElseThrow(() -> new MyNotFoundException("User not found"));
        UserEntity userEntity = userConverter.toEntity(userBoundary);
        if (userEntity.getRole() != UserRole.ADMIN) {
            throw new MyForbiddenException("Only ADMIN users can delete all objects");
        }

        // Delete all objects
        this.objectCrud.deleteAll();
    }

    @Override
    @Transactional(readOnly = false)
    public void bindObjects(String parentSystemID, String parentObjectID, String childSystemID, String childObjectID,
                            String userSystemID, String userEmail) {
        // Verify user exists and get their role
        UserBoundary userBoundary = userService.login(userSystemID, userEmail)
                .orElseThrow(() -> new MyNotFoundException("User not found"));
        UserEntity userEntity = userConverter.toEntity(userBoundary);

        // Only OPERATOR users can bind objects
        if (userEntity.getRole() != UserRole.OPERATOR) {
            throw new MyForbiddenException("Can't bind objects");
        }

        // get parent entity by parentId if exists
        // otherwise return 404 status
        ObjectEntity parent = this.objectCrud
                .findById(parentObjectID)
                .orElseThrow(()->new MyNotFoundException("Could not find parent entity by id: " + parentObjectID));

        // get child entity by childId if exists
        // otherwise return 404 status
        ObjectEntity child = this.objectCrud
                .findById(childObjectID)
                .orElseThrow(()->new MyNotFoundException("Could not find child entity by id: " + childObjectID));

        // connect entities and store updates to database
        child.setParent(parent);

        this.objectCrud
                .save(child);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ObjectBoundary> getParent(String childSystemID, String childObjectID, String userSystemID, String userEmail, int size, int page) {
        // Verify user exists and get their role
        UserBoundary userBoundary = userService.login(userSystemID, userEmail)
                .orElseThrow(() -> new MyNotFoundException("User not found"));
        UserEntity userEntity = userConverter.toEntity(userBoundary);

        // Get the child entity to check if it exists
        ObjectEntity child = this.objectCrud
                .findById(childObjectID)
                .orElseThrow(() -> new MyNotFoundException("Could not find child by id: " + childObjectID));

        // Apply permission rules based on user role
        switch (userEntity.getRole()) {
            case ADMIN:
                // ADMIN users have no access
                throw new MyForbiddenException("Object not found");

            case OPERATOR:
                // OPERATOR users have full access
                // If there's a parent and we're on the first page, return it
                if (child.getParent() != null && page == 0 && size > 0) {
                    return Collections.singletonList(converter.toBoundary(child.getParent()));
                }
                // Otherwise return empty list
                return Collections.emptyList();

            case END_USER:
                // END_USER can only access active objects
                if (child.getActive() == null || !child.getActive() || !child.getParent().getActive()) {
                    throw new MyForbiddenException("Object not found");
                }

                // For END_USER, check if parent exists and is active
                if (child.getParent() != null &&
                        child.getParent().getActive() != null &&
                        child.getParent().getActive() &&
                        page == 0 && size > 0) {
                    return Collections.singletonList(converter.toBoundary(child.getParent()));
                }
                // Otherwise return empty list
                return Collections.emptyList();

            default:
                throw new MyForbiddenException("Unknown user role");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ObjectBoundary> getChildren(String parentSystemID, String parentObjectID, String userSystemID, String userEmail, int size, int page) {
        // Verify user exists and get their role
        UserBoundary userBoundary = userService.login(userSystemID, userEmail)
                .orElseThrow(() -> new MyNotFoundException("User not found"));
        UserEntity userEntity = userConverter.toEntity(userBoundary);

        // Check if parent object exists
        ObjectEntity parent = this.objectCrud
                .findById(parentObjectID)
                .orElseThrow(() -> new MyNotFoundException("Could not find parent by id: " + parentObjectID));

        // Apply permission rules based on user role
        switch (userEntity.getRole()) {
            case ADMIN:
                // ADMIN users have no access
                throw new MyForbiddenException("Object not found");

            case OPERATOR:
                // OPERATOR users have full access to all objects
                // Use pagination for the query
                return this.objectCrud
                        .findAllByParent_id(parentObjectID, PageRequest.of(page, size, Sort.Direction.ASC, "creationTimestamp", "id"))
                        .stream()
                        .map(this.converter::toBoundary)
                        .collect(Collectors.toList());

            case END_USER:
                // END_USER can only access active objects
                if (parent.getActive() == null || !parent.getActive()) {
                    throw new MyForbiddenException("Object not found");
                }

                // For END_USER, get paginated results but filter to only active children
                return this.objectCrud
                        .findAllByParent_id(parentObjectID, PageRequest.of(page, size, Sort.Direction.ASC, "creationTimestamp", "id"))
                        .stream()
                        .filter(entity -> entity.getActive() != null && entity.getActive())
                        .map(this.converter::toBoundary)
                        .collect(Collectors.toList());

            default:
                throw new MyForbiddenException("Unknown user role");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ObjectBoundary> searchObjectsByExactAlias(String alias, String userSystemID, String userEmail, int size, int page) {
        // Validate input
        if (alias == null || alias.trim().isEmpty()) {
            throw new MyInvalidInputException("Alias cannot be null or empty");
        }

        // Verify user exists and get their role
        UserBoundary userBoundary = userService.login(userSystemID, userEmail)
                .orElseThrow(() -> new MyNotFoundException("User not found"));
        UserEntity userEntity = userConverter.toEntity(userBoundary);

        // Apply permission rules based on user role
        switch (userEntity.getRole()) {
            case ADMIN:
                // ADMIN users have no access
                throw new MyForbiddenException("Object not found");

            case OPERATOR:
                // OPERATOR users have full access to all objects
                return this.objectCrud
                        .findAllByAlias(alias, PageRequest.of(page, size, Sort.Direction.ASC, "creationTimestamp", "id"))
                        .stream()
                        .map(converter::toBoundary)
                        .collect(Collectors.toList());

            case END_USER:
                // END_USER can only access active objects
                return this.objectCrud
                        .findAllByAlias(alias, PageRequest.of(page, size, Sort.Direction.ASC, "creationTimestamp", "id"))
                        .stream()
                        .filter(entity -> entity.getActive() != null && entity.getActive())
                        .map(converter::toBoundary)
                        .collect(Collectors.toList());

            default:
                throw new MyForbiddenException("Unknown user role");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ObjectBoundary> searchObjectsByAliasPattern(String pattern, String userSystemID, String userEmail, int size, int page) {
        // Validate input
        if (pattern == null || pattern.trim().isEmpty()) {
            throw new MyInvalidInputException("Pattern cannot be null or empty");
        }

        // Verify user exists and get their role
        UserBoundary userBoundary = userService.login(userSystemID, userEmail)
                .orElseThrow(() -> new MyNotFoundException("User not found"));
        UserEntity userEntity = userConverter.toEntity(userBoundary);

        // Apply permission rules based on user role
        switch (userEntity.getRole()) {
            case ADMIN:
                // ADMIN users have no access
                throw new MyForbiddenException("Object not found");

            case OPERATOR:
                // OPERATOR users have full access to all objects
                return this.objectCrud
                        .findAllByAliasLike(
                                "*" + pattern + "*",
                                PageRequest.of(page, size, Sort.Direction.ASC, "creationTimestamp", "id"))
                        .stream()
                        .map(this.converter::toBoundary)
                        .collect(Collectors.toList());

            case END_USER:
                // END_USER can only access active objects
                return this.objectCrud
                        .findAllByAliasLike(
                                "*" + pattern + "*",
                                PageRequest.of(page, size, Sort.Direction.ASC, "creationTimestamp", "id"))
                        .stream()
                        .filter(entity -> entity.getActive() != null && entity.getActive())
                        .map(this.converter::toBoundary)
                        .collect(Collectors.toList());

            default:
                throw new MyForbiddenException("Unknown user role");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ObjectBoundary> searchObjectsByType(String type, String userSystemID, String userEmail, int size, int page) {
        // Validate input
        if (type == null || type.trim().isEmpty()) {
            throw new MyInvalidInputException("Type cannot be null or empty");
        }

        // Verify user exists and get their role
        UserBoundary userBoundary = userService.login(userSystemID, userEmail)
                .orElseThrow(() -> new MyNotFoundException("User not found"));
        UserEntity userEntity = userConverter.toEntity(userBoundary);

        // Apply permission rules based on user role
        switch (userEntity.getRole()) {
            case ADMIN:
                // ADMIN users have no access
                throw new MyForbiddenException("Object not found");

            case OPERATOR:
                // OPERATOR users have full access to all objects
                return this.objectCrud
                        .findAllByType(type, PageRequest.of(page, size, Sort.Direction.ASC, "creationTimestamp", "id"))
                        .stream()
                        .map(converter::toBoundary)
                        .collect(Collectors.toList());

            case END_USER:
                // END_USER can only access active objects
                return this.objectCrud
                        .findAllByType(type, PageRequest.of(page, size, Sort.Direction.ASC, "creationTimestamp", "id"))
                        .stream()
                        .filter(entity -> entity.getActive() != null && entity.getActive())
                        .map(converter::toBoundary)
                        .collect(Collectors.toList());

            default:
                throw new MyForbiddenException("Unknown user role");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ObjectBoundary> searchObjectsByStatus(String status, String userSystemID, String userEmail, int size, int page) {
        // Validate input
        if (status == null) {
            throw new MyInvalidInputException("Status cannot be null");
        }

        // Verify user exists and get their role
        UserBoundary userBoundary = userService.login(userSystemID, userEmail)
                .orElseThrow(() -> new MyNotFoundException("User not found"));
        UserEntity userEntity = userConverter.toEntity(userBoundary);

        // Apply permission rules based on user role
        switch (userEntity.getRole()) {
            case ADMIN:
                // ADMIN users have no access
                throw new MyForbiddenException("Object not found");

            case OPERATOR:
                // OPERATOR users have full access to all objects
                return this.objectCrud
                        .findAllByStatus(status, PageRequest.of(page, size, Sort.Direction.ASC, "creationTimestamp", "id"))
                        .stream()
                        .map(converter::toBoundary)
                        .collect(Collectors.toList());

            case END_USER:
                // END_USER can only access active objects
                return this.objectCrud
                        .findAllByStatus(status, PageRequest.of(page, size, Sort.Direction.ASC, "creationTimestamp", "id"))
                        .stream()
                        .filter(entity -> entity.getActive() != null && entity.getActive())
                        .map(converter::toBoundary)
                        .collect(Collectors.toList());

            default:
                throw new MyForbiddenException("Unknown user role");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ObjectBoundary> searchObjectsByTypeAndStatus(String type, String status, String userSystemID, String userEmail, int size, int page) {
        // Validate input
        if (type == null || type.trim().isEmpty()) {
            throw new MyInvalidInputException("Type cannot be null or empty");
        }

        if (status == null) {
            throw new MyInvalidInputException("Status cannot be null");
        }

        // Verify user exists and get their role
        UserBoundary userBoundary = userService.login(userSystemID, userEmail)
                .orElseThrow(() -> new MyNotFoundException("User not found"));
        UserEntity userEntity = userConverter.toEntity(userBoundary);

        // Apply permission rules based on user role
        switch (userEntity.getRole()) {
            case ADMIN:
                // ADMIN users have no access
                throw new MyForbiddenException("Object not found");

            case OPERATOR:
                // OPERATOR users have full access to all objects
                return this.objectCrud
                        .findAllByTypeAndStatus(type, status, PageRequest.of(page, size, Sort.Direction.ASC, "creationTimestamp", "id"))
                        .stream()
                        .map(converter::toBoundary)
                        .collect(Collectors.toList());

            case END_USER:
                // END_USER can only access active objects
                return this.objectCrud
                        .findAllByTypeAndStatus(type, status, PageRequest.of(page, size, Sort.Direction.ASC, "creationTimestamp", "id"))
                        .stream()
                        .filter(entity -> entity.getActive() != null && entity.getActive())
                        .map(converter::toBoundary)
                        .collect(Collectors.toList());

            default:
                throw new MyForbiddenException("Unknown user role");
        }
    }
}