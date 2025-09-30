package demo.DataAccessLayer.CRUDs;

import demo.DataAccessLayer.Entities.UserEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserCrud extends MongoRepository<UserEntity, String> {

}