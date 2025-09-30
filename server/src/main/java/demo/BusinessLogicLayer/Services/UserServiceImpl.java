package demo.BusinessLogicLayer.Services;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import demo.Enums.UserRole;
import demo.PresentationLayer.Boundaries.NewUserBoundary;
import demo.PresentationLayer.Boundaries.UserBoundary;
import demo.DataAccessLayer.CRUDs.UserCrud;
import demo.BusinessLogicLayer.Converters.UserConverter;
import demo.DataAccessLayer.Entities.UserEntity;
import demo.BusinessLogicLayer.Exceptions.MyForbiddenException;
import demo.BusinessLogicLayer.Exceptions.MyInvalidInputException;
import demo.BusinessLogicLayer.Exceptions.MyNotFoundException;
import demo.DataAccessLayer.IDs.UserID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private UserCrud userCrud;
    private UserConverter userConverter;
    private String appName;

    public UserServiceImpl(
            UserCrud userCrud,
            UserConverter userConverter) {
        this.userCrud = userCrud;
        this.userConverter = userConverter;
    }

    @Value("${spring.application.name:dummy}")
    public void setAppName(String appName) {
        this.appName = appName;
        System.err.println("*** " + this.appName);
    }

    @Override
    @Transactional
    public UserBoundary createUser(NewUserBoundary user) {
        // Validate input
        if (user == null) {
            throw new MyInvalidInputException("User data cannot be null");
        }

        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            throw new MyInvalidInputException("Email cannot be empty");
        }

        if (!isValidEmail(user.getEmail()) || user.getEmail().isEmpty()) {
            throw new MyInvalidInputException("Invalid email format");
        }

        if (user.getRole() == null) {
            throw new MyInvalidInputException("Role cannot be null");
        }

        if (user.getUsername() == null) {
            throw new MyInvalidInputException("User name cannot be null");
        }

        if (user.getUsername().getFirst() == null || user.getUsername().getFirst().trim().isEmpty()) {
            throw new MyInvalidInputException("First name cannot be empty");
        }

        if (user.getUsername().getLast() == null || user.getUsername().getLast().trim().isEmpty()) {
            throw new MyInvalidInputException("Last name cannot be empty");
        }

        // Check if user already exists
        String userId = user.getEmail() + "/" + this.appName;
        if (this.userCrud.existsById(userId)) {
            throw new MyInvalidInputException("User with email " + user.getEmail() + " already exists");
        }

        // Convert NewUserBoundary to UserBoundary first
        UserBoundary userBoundary = new UserBoundary();

        // Set UserID
        UserID userID = new UserID();
        userID.setEmail(user.getEmail());
        userID.setSystemID(this.appName);
        userBoundary.setUserId(userID);

        // Copy other fields from NewUserBoundary
        userBoundary.setRole(user.getRole());
        userBoundary.setUsername(user.getUsername());
        userBoundary.setAvatar(user.getAvatar());

        // Use the converter to convert UserBoundary to UserEntity
        UserEntity entity = this.userConverter.toEntity(userBoundary);

        // Add email since it's not part of UserBoundary but needed in entity
        entity.setEmail(user.getEmail());

        // Save to database
        entity = this.userCrud.save(entity);

        // Convert back to boundary using the converter
        return this.userConverter.toBoundary(entity);
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pattern = Pattern.compile(emailRegex);
        return pattern.matcher(email).matches();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserBoundary> getAllUsers(String systemId, String email, int size, int page) {
        String userId = email + "/" + systemId;
        Optional<UserEntity> optionalUser = this.userCrud.findById(userId);

        if (optionalUser.isEmpty()) {
            throw new MyForbiddenException("User with ID system: " + systemId + "/" + email + "does not exist or credentials are invalid");
        }

        UserEntity userEntity = optionalUser.get();
        if (userEntity.getRole() != UserRole.ADMIN) {
            throw new MyForbiddenException("Only ADMIN users can retrieve all users");
        }

        return this.userCrud.findAll(PageRequest.of(page, size, Sort.Direction.ASC, "lastName" , "firstName", "userId"))
                .stream()
                .map(this.userConverter::toBoundary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserBoundary> login(String systemId, String userEmail) {
        String userId = userEmail + "/" + systemId;
        Optional<UserEntity> optionalUser = this.userCrud.findById(userId);

        if (optionalUser.isEmpty()) {
            throw new MyForbiddenException("User with ID " + userEmail + "/" + systemId + " does not exist or credentials are invalid");
        }

        return optionalUser.map(this.userConverter::toBoundary);
    }

    @Override
    @Transactional
    public void updateUser(String systemId, String userEmail, UserBoundary update) {
        String userId = userEmail + "/" + systemId;
        Optional<UserEntity> optionalUser = this.userCrud.findById(userId);

        if (optionalUser.isEmpty()) {
            throw new MyNotFoundException("Could not find user with email: " + userEmail);
        }

        UserEntity existing = optionalUser.get();

        // Validate update data
        if (update == null) {
            throw new MyInvalidInputException("Update data cannot be null");
        }

        // Update fields if provided
        if (update.getUsername() != null) {
            if (update.getUsername().getFirst() != null) {
                existing.setFirstName(update.getUsername().getFirst());
            }

            if (update.getUsername().getLast() != null) {
                existing.setLastName(update.getUsername().getLast());
            }
        }

        if (update.getAvatar() != null) {
            existing.setAvatar(update.getAvatar());
        }

        if (update.getRole() != null) {
            existing.setRole(update.getRole());
        }

        // Save updated entity
        this.userCrud.save(existing);
    }

    @Override
    @Transactional
    public void deleteAllUsers(String systemId, String userEmail) {
        // Authenticate and check role
        UserBoundary userBoundary = this.login(systemId, userEmail)
                .orElseThrow(() -> new MyNotFoundException("User not found"));
        UserEntity userEntity = userConverter.toEntity(userBoundary);
        if (userEntity.getRole() != UserRole.ADMIN) {
            throw new MyForbiddenException("Only ADMIN users can delete all users");
        }

        this.userCrud.deleteAll();
    }


}