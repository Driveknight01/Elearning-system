package com.elearning.module.group.entity;

import com.elearning.module.flashcard.entity.FlashcardSet;
import com.elearning.module.user.entity.User;
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
 * Flashcard set shared inside a peer study group, mapped to {@code study_group_sets}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "study_group_sets")
public class StudyGroupSet implements Persistable<StudyGroupSetId> {

    @Builder.Default
    @EmbeddedId
    private StudyGroupSetId id = new StudyGroupSetId();

    @MapsId("groupId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private StudyGroup studyGroup;

    @MapsId("setId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "set_id", nullable = false)
    private FlashcardSet flashcardSet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shared_by", nullable = false)
    private User sharedBy;

    @CreatedDate
    @Column(name = "shared_at", nullable = false, updatable = false)
    private Instant sharedAt;

    @Builder.Default
    @Transient
    private boolean newEntity = true;

    @Override
    public StudyGroupSetId getId() {
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
        if (this.id == null && this.studyGroup != null && this.flashcardSet != null) {
            this.id = new StudyGroupSetId(this.studyGroup.getId(), this.flashcardSet.getId());
        }
        if (this.sharedAt == null) {
            this.sharedAt = Instant.now();
        }
    }
}
