package demo.DataAccessLayer.CRUDs;

import demo.DataAccessLayer.Entities.ObjectEntity;

import java.util.List;

import org.springframework.data.domain.Pageable;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;


public interface ObjectCrud extends MongoRepository<ObjectEntity, String> {

    public List<ObjectEntity> findAllByParent_id(
            @Param("parentId") String parentId,
            Pageable pageable);

    public List<ObjectEntity> findAllByAlias(
            @Param("alias") String alias,
            Pageable pageable);

    public List<ObjectEntity> findAllByAliasLike(
            @Param("pattern") String pattern,
            Pageable pageable);

    public List<ObjectEntity> findAllByType(
            @Param("type") String type,
            Pageable pageable);

    public List<ObjectEntity> findAllByStatus(
            @Param("status") String status,
            Pageable pageable);

    public List<ObjectEntity> findAllByTypeAndStatus(
            @Param("type") String type,
            @Param("status") String status,
            Pageable pageable);

}


