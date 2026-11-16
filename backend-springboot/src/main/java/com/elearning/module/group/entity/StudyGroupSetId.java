package com.elearning.module.group.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Composite primary key for {@link StudyGroupSet} ({@code group_id}, {@code set_id}).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class StudyGroupSetId implements Serializable {

    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "set_id")
    private Long setId;
}
