package demo.DataAccessLayer.IDs;

public class CommandID {
    private String commandID;
    private String systemID;
    
    public CommandID() {
    }
    
    public CommandID(String commandID, String systemID) {
        this.commandID = commandID;
        this.systemID = systemID;
    }
    
    public String getCommandID() {
        return commandID;
    }
    
    public void setCommandID(String commandId) {
        this.commandID = commandId;
    }
    
    public String getSystemID() {
        return systemID;
    }
    
    public void setSystemID(String systemId) {
        this.systemID = systemId;
    }
    
    @Override
    public String toString() {
        return systemID + '/' + commandID;
    }
}