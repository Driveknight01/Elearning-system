package com.elearning.module.flashcard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Composite primary key for {@link FolderSet} ({@code folder_id}, {@code set_id}).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class FolderSetId implements Serializable {

    @Column(name = "folder_id")
    private Long folderId;

    @Column(name = "set_id")
    private Long setId;
}
