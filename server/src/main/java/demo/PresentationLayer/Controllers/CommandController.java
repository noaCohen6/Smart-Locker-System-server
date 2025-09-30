package demo.PresentationLayer.Controllers;

import demo.PresentationLayer.Boundaries.CommandBoundary;
import demo.BusinessLogicLayer.Services.CommandService;
import demo.PresentationLayer.Boundaries.ObjectBoundary;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "ambient-intelligence/commands")
public class CommandController {

    private CommandService commandService;

    public CommandController(CommandService commandService) {
        this.commandService = commandService;
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Object> invokeCommand(@RequestBody CommandBoundary command) {
        return this.commandService.invokeCommand(command);
    }
}