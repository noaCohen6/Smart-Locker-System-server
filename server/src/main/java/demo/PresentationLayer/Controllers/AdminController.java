package demo.PresentationLayer.Controllers;

import java.util.List;

import demo.PresentationLayer.Boundaries.UserBoundary;
import demo.BusinessLogicLayer.Services.ObjectService;
import demo.BusinessLogicLayer.Services.UserService;
import org.springframework.context.annotation.ScopeMetadata;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import demo.PresentationLayer.Boundaries.CommandBoundary;
import demo.BusinessLogicLayer.Services.CommandService;

@RestController
@RequestMapping(path = "/ambient-intelligence/admin")
public class AdminController {

    private UserService userService;
    private CommandService commandService;
    private ObjectService objectService;

    public AdminController(
            UserService userService,
            CommandService commandService,
            ObjectService objectService) {
        this.userService = userService;
        this.commandService = commandService;
        this.objectService = objectService;
    }

    // Delete all users in the system
    @DeleteMapping(path = "/users")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllUsers(
            @RequestParam("userSystemID") String systemID,
            @RequestParam("userEmail") String email) {
        this.userService.deleteAllUsers(systemID, email);
    }

    // Delete all objects in the system
    @DeleteMapping(path = "/objects")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllObjects(
            @RequestParam("userSystemID") String systemID,
            @RequestParam("userEmail") String email) {
        this.objectService.deleteAllObjects(systemID, email);
    }
    // Delete all commands history
    @DeleteMapping(path = "/commands")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllCommands(
            @RequestParam("userSystemID") String systemID,
            @RequestParam("userEmail") String email) {

        this.commandService.deleteAllCommands(systemID, email);
    }

    // Export all users

    @GetMapping(
            path = "/users",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public UserBoundary[] getAllUsers(
            @RequestParam("userSystemID") String systemID,
            @RequestParam("userEmail") String email,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page) {

        List<UserBoundary> users = this.userService.getAllUsers(systemID, email, size, page);
        return users.toArray(new UserBoundary[0]);
    }

    // Export all Commands history
    @GetMapping(
            path = "/commands",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public CommandBoundary[] getAllCommands(
            @RequestParam("userSystemID") String systemID,
            @RequestParam("userEmail") String email,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page) {

        List<CommandBoundary> commands = this.commandService.getAllCommandsHistory(systemID, email, size, page);
        return commands.toArray(new CommandBoundary[0]);

    }
}