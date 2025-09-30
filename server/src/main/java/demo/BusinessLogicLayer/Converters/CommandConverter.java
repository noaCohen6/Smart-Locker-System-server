package demo.BusinessLogicLayer.Converters;

import demo.PresentationLayer.Boundaries.CommandBoundary;
import demo.DataAccessLayer.Entities.CommandEntity;
import demo.DataAccessLayer.IDs.CommandID;
import demo.DataAccessLayer.IDs.UserID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CommandConverter {
    @Value("${spring.application.name:dummy}")
    private String applicationName;
    public CommandBoundary toBoundary(CommandEntity entity) {
        CommandBoundary rv = new CommandBoundary();

        if (entity.getCommandID() != null) {
            CommandID commandID = new CommandID();
            commandID.setSystemID(applicationName);
            commandID.setCommandID(entity.getCommandID());
            rv.setCommandID(commandID);
        }
        rv.setCommand(entity.getCommand());
        rv.setTargetObject(entity.getTargetObject());
        if (entity.getInvocationTimestamp() != null) {
            rv.setInvocationTimestamp(entity.getInvocationTimestamp());
        }

        // Fix: Properly convert the invokedBy string back to a UserID object
        if (entity.getInvokedBy() != null) {
            String[] parts = entity.getInvokedBy().split("/");
            if (parts.length == 2) {
                // The format is expected to be email/systemId
                UserID userId = new UserID(parts[0], parts[1]);
                Map<String, UserID> invokedByMap = new HashMap<>();
                invokedByMap.put("userId", userId);
                rv.setInvokedBy(invokedByMap);
            }
        }

        rv.setCommandAttributes(entity.getCommandAttributes());

        return rv;
    }

    public CommandEntity toEntity(CommandBoundary boundary) {
        CommandEntity rv = new CommandEntity();

        if (boundary.getCommandID() != null) {
            rv.setCommandID(boundary.getCommandID().getCommandID());
            rv.setSystemID(boundary.getCommandID().getSystemID());
        }

        rv.setCommand(boundary.getCommand());
        rv.setTargetObject(boundary.getTargetObject());

        if (boundary.getInvocationTimestamp() != null) {
            rv.setInvocationTimestamp(boundary.getInvocationTimestamp());
        }

        if (boundary.getInvokedBy() != null && boundary.getInvokedBy().containsKey("userId")) {
            UserID userId = boundary.getInvokedBy().get("userId");
            // Convert UserID to string format: email/systemId
            rv.setInvokedBy(userId.getEmail() + "/" + userId.getSystemID());
        }

        rv.setCommandAttributes(boundary.getCommandAttributes());

        return rv;
    }
}