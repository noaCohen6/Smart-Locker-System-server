package demo.BusinessLogicLayer.Services;


import demo.PresentationLayer.Boundaries.NewUserBoundary;
import demo.PresentationLayer.Boundaries.UserBoundary;

import java.util.List;
import java.util.Optional;

public interface UserService {

     UserBoundary createUser(NewUserBoundary user);

     List<UserBoundary> getAllUsers(String systemId, String userEmail, int size, int page);

     Optional<UserBoundary> login(String systemId, String userEmail);

     void updateUser(String systemId, String userEmail, UserBoundary update);

     void deleteAllUsers(String systemId, String userEmail);
}