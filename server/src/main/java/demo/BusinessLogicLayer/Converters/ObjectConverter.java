package demo.BusinessLogicLayer.Converters;

import demo.PresentationLayer.Boundaries.ObjectBoundary;
import demo.DataAccessLayer.Entities.ObjectEntity;
import demo.DataAccessLayer.IDs.ObjectID;
import demo.DataAccessLayer.IDs.UserID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ObjectConverter {
    @Value("${spring.application.name:dummy}")
    private String applicationName;
    public ObjectBoundary toBoundary(ObjectEntity entity) {
        ObjectBoundary rv = new ObjectBoundary();

        if (entity.getId() != null) {
            ObjectID objectID = new ObjectID();
            objectID.setSystemID(applicationName);
            objectID.setID(entity.getId());
            rv.setObjectID(objectID);
        }
        rv.setType(entity.getType());
        rv.setAlias(entity.getAlias());
        rv.setStatus(entity.getStatus());
        rv.setActive(entity.getActive());
        if (entity.getCreationTimestamp() != null) {
            rv.setCreationTimestamp(entity.getCreationTimestamp());
        }

        if (entity.getCreatedBy() != null && !entity.getCreatedBy().isEmpty()) {
            String[] parts = entity.getCreatedBy().split("/");
            if (parts.length == 2) {
                UserID userId = new UserID(parts[0], parts[1]);
                Map<String, UserID> createdByMap = new HashMap<>();
                createdByMap.put("userId", userId);
                rv.setCreatedBy(createdByMap);
            }
        }
        rv.setObjectDetails(entity.getObjectDetails());


        return rv;
    }

    public ObjectEntity toEntity(ObjectBoundary boundary) {
        ObjectEntity rv = new ObjectEntity();

        if (boundary.getObjectID() != null) {
            rv.setId(boundary.getObjectID().toString());
        }else {
            rv.setId(null);
        }
        rv.setType(boundary.getType());
        rv.setAlias(boundary.getAlias());
        rv.setStatus(boundary.getStatus());
        rv.setActive(boundary.getActive());
        rv.setCreationTimestamp(boundary.getCreationTimestamp());
        if (boundary.getCreatedBy() != null && boundary.getCreatedBy().containsKey("userId")) {
            UserID userId = boundary.getCreatedBy().get("userId");
            rv.setCreatedBy(userId.getEmail() + "/" + userId.getSystemID());
        }
        rv.setObjectDetails(boundary.getObjectDetails());
        return rv;
    }
}
