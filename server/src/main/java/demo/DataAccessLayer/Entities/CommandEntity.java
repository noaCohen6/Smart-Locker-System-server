
package demo.DataAccessLayer.Entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


import java.util.Date;
import java.util.Map;

@Document(collection = "COMMAND")
public class CommandEntity {
    @Id
    private String commandID;
    private String systemID;
    private String command; // The command type (e.g., "echo", "create", "update", "delete", "get")
    private Map<String, Object> targetObject; // The object this command targets
    private Date invocationTimestamp; // When the command was executed
    private String invokedBy; // Who executed the command
    private Map<String, Object> commandAttributes; // Additional command parameters

    public CommandEntity() {
        }

        public String getCommandID() {
            return commandID;
    }

    public void setCommandID(String commandID) {
        this.commandID = commandID;
    }

    public String getSystemID() {
        return systemID;
    }

    public void setSystemID(String systemID) {
        this.systemID = systemID;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public Map<String, Object> getTargetObject() {
        return targetObject;
    }

    public void setTargetObject(Map<String, Object> targetObject) {
        this.targetObject = targetObject;
    }

    public Date getInvocationTimestamp() {
        return invocationTimestamp;
    }

    public void setInvocationTimestamp(Date invocationTimestamp) {
        this.invocationTimestamp = invocationTimestamp;
    }

        public String getInvokedBy() {
        return invokedBy;
    }

    public void setInvokedBy(String invokedBy) {
        this.invokedBy = invokedBy;
    }

    public Map<String, Object> getCommandAttributes() {
        return commandAttributes;
    }

    public void setCommandAttributes(Map<String, Object> commandAttributes) {
        this.commandAttributes = commandAttributes;
    }

    @Override
    public String toString() {
        return "CommandEntity{" +
                "commandId='" + commandID + '\'' +
                ", systemId='" + systemID + '\'' +
                ", command='" + command + '\'' +
                ", targetObject=" + targetObject +
                ", invocationTimestamp=" + invocationTimestamp +
                ", invokedBy=" + invokedBy +
                ", commandAttributes=" + commandAttributes +
                '}';
    }
}