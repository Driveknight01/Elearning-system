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
 * Composite primary key for {@link StarredCard} ({@code user_id}, {@code card_id}).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class StarredCardId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "card_id")
    private Long cardId;
}
