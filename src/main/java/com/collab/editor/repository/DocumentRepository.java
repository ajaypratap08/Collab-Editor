package com.collab.editor.repository;

import com.collab.editor.model.Document;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends MongoRepository<Document,String> {
    List<Document> findByOwnerId(String ownerId);
    List<Document> findByCollaboratorsContaining(String userId);


}
