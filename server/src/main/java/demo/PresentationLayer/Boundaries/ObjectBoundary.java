package demo.PresentationLayer.Boundaries;

import demo.DataAccessLayer.IDs.ObjectID;
import demo.DataAccessLayer.IDs.UserID;


import java.util.Date;
import java.util.Map;

public class ObjectBoundary {
    private ObjectID objectID;
    private String type;
    private String alias;
    private String status;
    private Boolean active;
    private Date creationTimestamp;
    private Map<String, UserID> createdBy;
    private Map<String, Object> objectDetails;

    public ObjectBoundary()
    {

    }

    public ObjectID getObjectID() {
        return objectID;
    }

    public void setObjectID(ObjectID objectID) {
        this.objectID = objectID;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Date getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(Date creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public Map<String, UserID> getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Map<String, UserID> createdBy) {
        this.createdBy = createdBy;
    }

    public Map<String, Object> getObjectDetails() {
        return objectDetails;
    }

    public void setObjectDetails(Map<String, Object> objectDetails) {
        this.objectDetails = objectDetails;
    }

    @Override
    public String toString() {
        return "ObjectBoundary{" +
                "objectID:" + objectID +
                ", type:" + type +
                ", alias:" + alias +
                ", status:" + status +
                ", active:" + active +
                ", creationTimestamp:" + creationTimestamp +
                ", createdBy:" + createdBy +
                ", objectDetails:" + objectDetails +
                '}';
    }
}
