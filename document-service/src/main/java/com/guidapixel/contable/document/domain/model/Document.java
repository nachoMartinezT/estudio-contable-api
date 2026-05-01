package com.guidapixel.contable.document.domain.model;

import com.guidapixel.contable.shared.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Document extends BaseEntity {

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String fileType;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String uploadedBy;

    @Column(nullable = false)
    private boolean fromTenant;

    private String description;

    private LocalDateTime downloadedAt;

    @PrePersist
    void prePersist() {
        if (this.downloadedAt == null) {
            this.downloadedAt = LocalDateTime.now();
        }
    }
}
