package demo.BusinessLogicLayer.Services;

import demo.PresentationLayer.Boundaries.CommandBoundary;
import demo.PresentationLayer.Boundaries.ObjectBoundary;


import java.util.List;

public interface CommandService {

     List<Object> invokeCommand(CommandBoundary command);

     List<CommandBoundary> getAllCommandsHistory(String userSystemId, String userEmail, int size, int page);

     void deleteAllCommands(String userSystemId, String userEmail);

     List<ObjectBoundary> getAvailableLockersByLocation(double latitude, double longitude, double radius,
                                                        String userSystemId, String userEmail, int size, int page);

}