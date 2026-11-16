package com.elearning.module.flashcard.entity;

import com.elearning.common.entity.BaseEntity;
import com.elearning.module.admin.entity.Category;
import com.elearning.module.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Flashcard study set entity mapped to {@code flashcard_sets}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "flashcard_sets")
public class FlashcardSet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cloned_from_set_id")
    private FlashcardSet clonedFromSet;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private Visibility visibility = Visibility.PUBLIC;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 20)
    private ModerationStatus moderationStatus = ModerationStatus.APPROVED;

    @Builder.Default
    @Column(name = "is_featured", nullable = false)
    private boolean featured = false;

    @Column(name = "share_token", nullable = false, unique = true, length = 40)
    private String shareToken;

    @Builder.Default
    @Column(name = "term_lang", nullable = false, length = 10)
    private String termLang = "en";

    @Builder.Default
    @Column(name = "def_lang", nullable = false, length = 10)
    private String defLang = "vi";

    @Builder.Default
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags", nullable = false)
    private List<String> tags = new ArrayList<>();

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private SourceType sourceType = SourceType.MANUAL;

    @Builder.Default
    @Column(name = "card_count", nullable = false)
    private Integer cardCount = 0;

    @Builder.Default
    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    @Builder.Default
    @Column(name = "clone_count", nullable = false)
    private Integer cloneCount = 0;

    @Builder.Default
    @Column(name = "avg_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal avgRating = new BigDecimal("0.00");

    @Builder.Default
    @Column(name = "rating_count", nullable = false)
    private Integer ratingCount = 0;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Builder.Default
    @OneToMany(mappedBy = "flashcardSet", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<Flashcard> cards = new ArrayList<>();

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void addCard(Flashcard card) {
        cards.add(card);
        card.setFlashcardSet(this);
    }

    public void removeCard(Flashcard card) {
        cards.remove(card);
        card.setFlashcardSet(null);
    }
}
