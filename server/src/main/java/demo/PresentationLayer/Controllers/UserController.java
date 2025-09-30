package demo.PresentationLayer.Controllers;

import demo.PresentationLayer.Boundaries.NewUserBoundary;
import demo.PresentationLayer.Boundaries.UserBoundary;
import demo.BusinessLogicLayer.Services.UserService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping(path = "/ambient-intelligence/users")
public class UserController {

    private UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Create a new user
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public UserBoundary createUser(@RequestBody NewUserBoundary newUser) {
        return this.userService.createUser(newUser);
    }

    // Login valid user and retrieve user details
    @GetMapping(
            path = "/login/{systemID}/{userEmail}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public UserBoundary login(
            @PathVariable("systemID") String systemID,
            @PathVariable("userEmail") String userEmail) {

        return this.userService
                .login(systemID, userEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Could not find user with email: " + userEmail));
    }

    // Update user details
    @PutMapping(
            path = "/{systemID}/{userEmail}",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public void updateUser(
            @PathVariable("systemID") String systemID,
            @PathVariable("userEmail") String userEmail,
            @RequestBody UserBoundary update) {

        this.userService.updateUser(systemID, userEmail, update);
    }
}