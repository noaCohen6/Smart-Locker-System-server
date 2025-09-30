package demo.DataAccessLayer.IDs;

public class ObjectID {
    private String id;
    private String systemID;

    public ObjectID() {
    }

    public ObjectID(String id, String systemId) {
        this.id = id;
        this.systemID = systemId;
    }

    public String getID() {
        return id;
    }

    public void setID(String id) {
        this.id = id;
    }

    public String getSystemID() {
        return systemID;
    }

    public void setSystemID(String systemId) {
        this.systemID = systemId;
    }

    @Override
    public String toString() {
        return systemID+ "/" + id;
    }
}
