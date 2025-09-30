package demo.BusinessLogicLayer.Converters;

import demo.PresentationLayer.Boundaries.UserBoundary;
import demo.PresentationLayer.Boundaries.UserNameBoundary;
import demo.Enums.UserRole;
import demo.DataAccessLayer.IDs.UserID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import demo.DataAccessLayer.Entities.UserEntity;


@Component
public class UserConverter {
  @Value("${spring.application.name:dummy}")
  private String applicationName;
  public UserBoundary toBoundary(UserEntity entity) {
      UserBoundary rv = new UserBoundary();
      if(entity.getUserId() != null) {
          UserID userId = new UserID();
          String email = entity.getEmail();
          userId.setEmail(email);
          userId.setSystemID(applicationName);
          rv.setUserId(userId);
      }

      String firstName = entity.getFirstName();
      String lastName = entity.getLastName();

      UserNameBoundary nameBoundary = new UserNameBoundary();

      nameBoundary.setFirst(firstName);
      nameBoundary.setLast(lastName);

      rv.setUsername(nameBoundary);

      UserRole role = entity.getRole();
      rv.setRole(role);

      String avatar = entity.getAvatar();
      rv.setAvatar(avatar);

      return rv;

  }

    public UserEntity toEntity(UserBoundary boundary) {
        UserEntity rv = new UserEntity();

        if(boundary.getUserId() != null) {
            String email = boundary.getUserId().getEmail();
            String systemId = applicationName;
            rv.setUserId(email + "/" + systemId );
        }else {
            rv.setUserId("");
        }

        String firstName = boundary.getUsername().getFirst();
        String lastName = boundary.getUsername().getLast();

        rv.setFirstName(firstName);
        rv.setLastName(lastName);

        String avatar = boundary.getAvatar();
        rv.setAvatar(avatar);

        UserRole role = boundary.getRole();
        rv.setRole(role);

        return rv;
    }

}
