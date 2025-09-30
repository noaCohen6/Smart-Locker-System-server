package demo.PresentationLayer.Boundaries;
import demo.DataAccessLayer.IDs.CommandID;
import demo.DataAccessLayer.IDs.UserID;

import java.util.Date;
import java.util.Map;

public class CommandBoundary {
    private CommandID commandID;
    private String command;
    private Map<String, Object> targetObject;
    private Date invocationTimestamp;
    private Map<String, UserID> invokedBy;
    private Map<String, Object> commandAttributes;

    public CommandBoundary() {
    }

    public CommandID getCommandID() {
        return commandID;
    }

    public void setCommandID(CommandID commandID) {
        this.commandID = commandID;
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

    public Map<String, UserID> getInvokedBy() {
        return invokedBy;
    }

    public void setInvokedBy(Map<String, UserID> invokedBy) {
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
        return "CommandBoundary{" +
                "id: " + commandID +
                ", command: " + command +
                ", targetObject: " + targetObject +
                ", invocationTimestamp: " + invocationTimestamp +
                ", invokedBy: " + invokedBy +
                ", commandAttributes: " + commandAttributes +
                '}';
    }
}