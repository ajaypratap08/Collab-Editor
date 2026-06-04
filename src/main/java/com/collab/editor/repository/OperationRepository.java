package com.collab.editor.repository;

import com.collab.editor.model.Operation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OperationRepository extends MongoRepository<Operation,String> {
    // get all ops for a document in order — needed for rollback
    List<Operation> findByDocumentIdOrderByTimestampAsc(String documentId);

    // get ops after a certain version — needed for catching up a late joiner
    List<Operation> findByDocumentIdAndDocumentVersionGreaterThanEqual(String documentId, int version);
}
