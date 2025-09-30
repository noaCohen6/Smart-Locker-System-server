package demo.BusinessLogicLayer.Services;

import demo.PresentationLayer.Boundaries.ObjectBoundary;

import java.util.List;
import java.util.Optional;

public interface ObjectService {

    ObjectBoundary createObject(ObjectBoundary object, Boolean fromCommand);

    void updateObject(String systemID, String objectID, String userSystemID, String userEmail, ObjectBoundary update,Boolean fromCommand);

    Optional<ObjectBoundary> getObjectById(String systemID, String objectId, String userSystemID, String userEmail);

    List<ObjectBoundary> getAllObjects(String userSystemID, String userEmail, int size, int page);

    void deleteAllObjects(String userSystemID, String userEmail);

    void bindObjects(String parentSystemID, String parentObjectID, String childSystemID, String childObjectID,
                     String userSystemID, String userEmail);

    List<ObjectBoundary> getChildren(String parentSystemID, String parentObjectID, String userSystemID, String userEmail, int size, int page);

    List<ObjectBoundary> getParent(String childSystemID, String childObjectID, String userSystemID, String userEmail, int size, int page);

    List<ObjectBoundary> searchObjectsByExactAlias(String alias, String userSystemID, String userEmail, int size, int page);

    List<ObjectBoundary> searchObjectsByAliasPattern(String pattern, String userSystemID, String userEmail, int size, int page);

    List<ObjectBoundary> searchObjectsByType(String type, String userSystemID, String userEmail, int size, int page);

    List<ObjectBoundary> searchObjectsByStatus(String status, String userSystemID, String userEmail, int size, int page);

    List<ObjectBoundary> searchObjectsByTypeAndStatus(String type, String status, String userSystemID, String userEmail, int size, int page);
}