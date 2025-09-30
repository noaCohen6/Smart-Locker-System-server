package demo.PresentationLayer.Controllers;

import demo.PresentationLayer.Boundaries.ObjectBoundary;
import demo.BusinessLogicLayer.Services.ObjectService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = { "ambient-intelligence/objects" })
public class ObjectController {
    private ObjectService objectService;

    public ObjectController(ObjectService objectService) {
        this.objectService = objectService;
    }

    @PostMapping(
            consumes = { MediaType.APPLICATION_JSON_VALUE },
            produces = { MediaType.APPLICATION_JSON_VALUE })
    public ObjectBoundary createObject(@RequestBody ObjectBoundary object) {
        System.err.println("createObject(" + object + ")");
        return this.objectService.createObject(object,false);
    }

    @GetMapping(produces = { MediaType.APPLICATION_JSON_VALUE })
    public ObjectBoundary[] getAllObjects(
            @RequestParam(name = "userSystemID", required = true) String userSystemID,
            @RequestParam(name = "userEmail", required = true) String userEmail,
            @RequestParam(name = "size", required = false, defaultValue = "5") Integer size,
            @RequestParam(name = "page", required = false, defaultValue = "0") Integer page) {

        return this.objectService
                .getAllObjects(userSystemID, userEmail, size, page)
                .toArray(new ObjectBoundary[0]);
    }

    @GetMapping(
            path = { "/{systemID}/{objectID}" },
            produces = { MediaType.APPLICATION_JSON_VALUE })
    public ObjectBoundary getSingleObject(
            @PathVariable("systemID") String systemID,
            @PathVariable("objectID") String objectID,
            @RequestParam(name = "userSystemID", required = true) String userSystemID,
            @RequestParam(name = "userEmail", required = true) String userEmail) {

        return this.objectService
                .getObjectById(systemID, objectID, userSystemID, userEmail)
                .orElseThrow(() ->
                        new RuntimeException("could not find object with id: " + systemID + "/" + objectID)
                );
    }

    @PutMapping(
            path = { "/{systemID}/{objectID}" },
            consumes = { MediaType.APPLICATION_JSON_VALUE })
    public void updateObject(
            @PathVariable("systemID") String systemID,
            @PathVariable("objectID") String objectID,
            @RequestParam(name = "userSystemID", required = true) String userSystemID,
            @RequestParam(name = "userEmail", required = true) String userEmail,
            @RequestBody ObjectBoundary update) {
        this.objectService.updateObject(systemID, objectID, userSystemID, userEmail, update,false);
    }

}