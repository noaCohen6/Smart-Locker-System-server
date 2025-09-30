package demo.DataAccessLayer.Entities;

    import org.springframework.data.annotation.Id;
    import org.springframework.data.mongodb.core.mapping.DBRef;
    import org.springframework.data.mongodb.core.mapping.Document;

    import java.util.Date;
    import java.util.Map;

    @Document(collection = "OBJECTS")
    public class ObjectEntity {
        @Id
        private String id;
        private String systemID;
        private String type;
        private String alias;
        private String status;
        private Boolean active;
        private Date creationTimestamp;
        private String createdBy;
        private Map<String, Object> objectDetails; // Using the same converter as MessageEntity

        @DBRef
        private ObjectEntity parent;

        public ObjectEntity() {
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSystemID() {
            return systemID;
        }

        public void setSystemID(String systemID) {
            this.systemID = systemID;
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


        public String getCreatedBy() {
            return createdBy;
        }

        public void setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
        }

        public Map<String, Object> getObjectDetails() {
            return objectDetails;
        }

        public void setObjectDetails(Map<String, Object> objectDetails) {
            this.objectDetails = objectDetails;
        }

        public ObjectEntity getParent() {
            return parent;
        }

        public void setParent(ObjectEntity parent) {
            this.parent = parent;
        }

        @Override
        public String toString() {
            return "ObjectEntity{" +
                    "objectID=" + id +
                    ", type='" + type + '\'' +
                    ", alias='" + alias + '\'' +
                    ", status=" + status +
                    ", active=" + active +
                    ", creationTimestamp=" + creationTimestamp +
                    ", createdBy=" + createdBy +
                    ", objectDetails=" + objectDetails +
                    '}';
        }
    }