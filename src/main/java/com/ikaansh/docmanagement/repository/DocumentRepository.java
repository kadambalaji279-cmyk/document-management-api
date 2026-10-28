package com.ikaansh.docmanagement.repository;

import com.ikaansh.docmanagement.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    // Owners are fetched with the documents to avoid an extra query per row when mapping responses.
    @Override
    @EntityGraph(attributePaths = "owner")
    Page<Document> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "owner")
    Page<Document> findByTitleContainingIgnoreCase(String title, Pageable pageable);
}
