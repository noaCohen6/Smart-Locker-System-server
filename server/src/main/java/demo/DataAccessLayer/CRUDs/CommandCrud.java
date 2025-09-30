package demo.DataAccessLayer.CRUDs;

import demo.DataAccessLayer.Entities.CommandEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CommandCrud extends MongoRepository<CommandEntity, String> {

}