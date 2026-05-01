package com.guidapixel.contable.document.domain.repository;

import com.guidapixel.contable.document.domain.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByTenantIdAndFromTenantTrue(Long tenantId);

    List<Document> findByTenantIdAndFromTenantFalse(Long tenantId);

    List<Document> findByTenantIdAndCategory(Long tenantId, String category);

    List<Document> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
}
