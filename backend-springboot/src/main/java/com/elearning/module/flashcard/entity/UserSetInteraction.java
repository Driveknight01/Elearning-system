package com.elearning.module.flashcard.entity;

import com.elearning.module.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Tracks saved sets, recent study access, 1-5 star ratings, and cross-platform resume state
 * mapped to {@code user_set_interactions}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_set_interactions")
public class UserSetInteraction implements Persistable<UserSetInteractionId> {

    @Builder.Default
    @EmbeddedId
    private UserSetInteractionId id = new UserSetInteractionId();

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @MapsId("setId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "set_id", nullable = false)
    private FlashcardSet flashcardSet;

    @Builder.Default
    @Column(name = "is_saved", nullable = false)
    private boolean saved = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_study_mode", length = 30)
    private StudyMode lastStudyMode;

    @Builder.Default
    @Column(name = "last_accessed_at", nullable = false)
    private Instant lastAccessedAt = Instant.now();

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resume_state_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> resumeStateJson = new HashMap<>();

    @Column(name = "rating")
    private Short rating;

    @Column(name = "review_comment", length = 500)
    private String reviewComment;

    @Column(name = "rated_at")
    private Instant ratedAt;

    @Builder.Default
    @Transient
    private boolean newEntity = true;

    @Override
    public UserSetInteractionId getId() {
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
        if (this.id == null && this.user != null && this.flashcardSet != null) {
            this.id = new UserSetInteractionId(this.user.getId(), this.flashcardSet.getId());
        }
        if (this.lastAccessedAt == null) {
            this.lastAccessedAt = Instant.now();
        }
        if (this.resumeStateJson == null) {
            this.resumeStateJson = new HashMap<>();
        }
    }
}
