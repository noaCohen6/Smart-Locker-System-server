package demo.PresentationLayer.Controllers;

import demo.PresentationLayer.Boundaries.ChildIdWrapper;
import demo.PresentationLayer.Boundaries.ObjectBoundary;
import demo.BusinessLogicLayer.Exceptions.MyNotFoundException;
import demo.DataAccessLayer.IDs.ObjectID;
import demo.BusinessLogicLayer.Services.ObjectService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(path = { "/objects" })
public class RelationController {
    private ObjectService objects;
    public RelationController(ObjectService objects) {
        this.objects = objects;
    }

    @PutMapping(path = { "/{parentSystemID}/{parentObjectID}/children" }, consumes = { MediaType.APPLICATION_JSON_VALUE })
    public void relateObjects(
            @PathVariable("parentSystemID") String parentSystemID,
            @PathVariable("parentObjectID") String parentObjectID,
            @RequestParam(name = "userSystemID", required = true) String userSystemID,
            @RequestParam(name = "userEmail", required = true) String userEmail,
            @RequestBody ChildIdWrapper childIdWrapper) {

        ObjectID childID = childIdWrapper.getChildId();

        this.objects.bindObjects(
                parentSystemID,
                parentObjectID,
                childID.getSystemID(),
                childID.getID(),
                userSystemID,
                userEmail);
    }

    @GetMapping(
            path = { "/{childSystemID}/{childObjectID}/parents" },
            produces = { MediaType.APPLICATION_JSON_VALUE })
    public ObjectBoundary[] getParents(
            @PathVariable("childSystemID") String childSystemID,
            @PathVariable("childObjectID") String childObjectID,
            @RequestParam(name = "userSystemID", required = true) String userSystemID,
            @RequestParam(name = "userEmail", required = true) String userEmail,
            @RequestParam(name = "size", required = false, defaultValue = "5") Integer size,
            @RequestParam(name = "page", required = false, defaultValue = "0") Integer page) {

        return this.objects
                .getParent(childSystemID, childObjectID, userSystemID, userEmail, size, page)
                .toArray(new ObjectBoundary[0]);
    }

    @GetMapping(
            path = { "/{parentSystemID}/{parentObjectID}/children" },
            produces = { MediaType.APPLICATION_JSON_VALUE })
    public ObjectBoundary[] getRelatedObjects(
            @PathVariable("parentSystemID") String systemID,
            @PathVariable("parentObjectID") String parentObjectID,
            @RequestParam(name = "userSystemID", required = true) String userSystemID,
            @RequestParam(name = "userEmail", required = true) String userEmail,
            @RequestParam(name = "size", required = false, defaultValue = "5") Integer size,
            @RequestParam(name = "page", required = false, defaultValue = "0") Integer page) {
        return this.objects
                .getChildren(systemID, parentObjectID, userSystemID, userEmail, size, page)
                .toArray(new ObjectBoundary[0]);
    }
}
