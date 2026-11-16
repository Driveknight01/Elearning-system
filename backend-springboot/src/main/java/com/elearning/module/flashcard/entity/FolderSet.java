package com.elearning.module.flashcard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Many-to-many join entity between {@link Folder} and {@link FlashcardSet} mapped to {@code folder_sets}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "folder_sets")
public class FolderSet implements Persistable<FolderSetId> {

    @Builder.Default
    @EmbeddedId
    private FolderSetId id = new FolderSetId();

    @MapsId("folderId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "folder_id", nullable = false)
    private Folder folder;

    @MapsId("setId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "set_id", nullable = false)
    private FlashcardSet flashcardSet;

    @CreatedDate
    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt;

    @Builder.Default
    @Transient
    private boolean newEntity = true;

    @Override
    public FolderSetId getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.newEntity = false;
    }

    @PrePersist
    void onPrePersist() {
        if (this.id == null && this.folder != null && this.flashcardSet != null) {
            this.id = new FolderSetId(this.folder.getId(), this.flashcardSet.getId());
        }
        if (this.addedAt == null) {
            this.addedAt = Instant.now();
        }
    }
}
