package com.elearning.smartflash.ui.set;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Mutable UI model representing a single editable flashcard row in {@link FlashcardEditAdapter}.
 */
public class EditableCardItem {

    private static final AtomicLong ID_GEN = new AtomicLong(1L);

    private final long localId;
    private Long id;
    private String term = "";
    private String definition = "";
    private String imageUrl;

    public EditableCardItem() {
        this.localId = ID_GEN.getAndIncrement();
    }

    public EditableCardItem(Long id, String term, String definition, String imageUrl) {
        this.localId = ID_GEN.getAndIncrement();
        this.id = id;
        this.term = term != null ? term : "";
        this.definition = definition != null ? definition : "";
        this.imageUrl = imageUrl;
    }

    public long getLocalId() {
        return localId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term != null ? term : "";
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition != null ? definition : "";
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
