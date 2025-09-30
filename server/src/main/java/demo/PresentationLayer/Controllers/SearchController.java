package demo.PresentationLayer.Controllers;


import demo.PresentationLayer.Boundaries.ObjectBoundary;
import demo.BusinessLogicLayer.Services.ObjectService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "ambient-intelligence/objects")
public class SearchController {
    private final ObjectService objectService;

    public SearchController(ObjectService objectService) {
        this.objectService = objectService;
    }

    @GetMapping(
            path = "/search/byAlias/{alias}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ObjectBoundary[] searchObjectsByExactAlias(
            @PathVariable("alias") String alias,
            @RequestParam(name = "userSystemID", required = true) String userSystemID,
            @RequestParam(name = "userEmail", required = true) String userEmail,
            @RequestParam(name = "size", required = false, defaultValue = "5") Integer size,
            @RequestParam(name = "page", required = false, defaultValue = "0") Integer page) {

        return this.objectService
                .searchObjectsByExactAlias(alias, userSystemID, userEmail, size, page)
                .toArray(new ObjectBoundary[0]);
    }

    @GetMapping(
            path = "/search/byAliasPattern/{pattern}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ObjectBoundary[] searchObjectsByAliasPattern(
            @PathVariable("pattern") String pattern,
            @RequestParam(name = "userSystemID", required = true) String userSystemID,
            @RequestParam(name = "userEmail", required = true) String userEmail,
            @RequestParam(name = "size", required = false, defaultValue = "5") Integer size,
            @RequestParam(name = "page", required = false, defaultValue = "0") Integer page) {

        return this.objectService
                .searchObjectsByAliasPattern(pattern, userSystemID, userEmail, size, page)
                .toArray(new ObjectBoundary[0]);
    }

    @GetMapping(
            path = "/search/byType/{type}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ObjectBoundary[] searchObjectsByType(
            @PathVariable("type") String type,
            @RequestParam(name = "userSystemID", required = true) String userSystemID,
            @RequestParam(name = "userEmail", required = true) String userEmail,
            @RequestParam(name = "size", required = false, defaultValue = "5") Integer size,
            @RequestParam(name = "page", required = false, defaultValue = "0") Integer page) {

        return this.objectService
                .searchObjectsByType(type, userSystemID, userEmail, size, page)
                .toArray(new ObjectBoundary[0]);
    }

    @GetMapping(
            path = "/search/byStatus/{status}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ObjectBoundary[] searchObjectsByStatus(
            @PathVariable("status") String status,
            @RequestParam(name = "userSystemID", required = true) String userSystemID,
            @RequestParam(name = "userEmail", required = true) String userEmail,
            @RequestParam(name = "size", required = false, defaultValue = "5") Integer size,
            @RequestParam(name = "page", required = false, defaultValue = "0") Integer page) {

        return this.objectService
                .searchObjectsByStatus(status, userSystemID, userEmail, size, page)
                .toArray(new ObjectBoundary[0]);
    }

    @GetMapping(
            path = "/search/byTypeAndStatus/{type}/{status}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ObjectBoundary[] searchObjectsByTypeAndStatus(
            @PathVariable("type") String type,
            @PathVariable("status") String status,
            @RequestParam(name = "userSystemID", required = true) String userSystemID,
            @RequestParam(name = "userEmail", required = true) String userEmail,
            @RequestParam(name = "size", required = false, defaultValue = "5") Integer size,
            @RequestParam(name = "page", required = false, defaultValue = "0") Integer page) {

        return this.objectService
                .searchObjectsByTypeAndStatus(type, status, userSystemID, userEmail, size, page)
                .toArray(new ObjectBoundary[0]);
    }
}