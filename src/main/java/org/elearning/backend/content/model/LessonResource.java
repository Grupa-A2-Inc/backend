package org.elearning.backend.content.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity class representing a resource associated with a lesson.
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "lesson_resources")
public class LessonResource {

    /**
     * Id of the resource
     * Generated automatically
     */
    @Id
    @Column(name = "id")
    @GeneratedValue
    private UUID id;

    /**
     * Id of the lesson associated with the resource
     * Not nullable, as every resource must be associated with a lesson
     * Many-to-one relationship with the Lesson entity, as multiple resources can be associated with a single lesson
     * Lazy fetching is used to optimize performance by loading the associated lesson only when needed
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    @JsonIgnore
    private Lesson lesson;

    /**
     * Title of the resource
     * Not nullable, as every resource must have a title
     */
    @NotNull
    @Column(name = "title")
    private String title;

    /**
    * URL of the resource
    * Not nullable, as every resource must have a URL
    */
    @NotNull
    @Column(name = "url")
    private String url;

    /**
     * Creation date of the resource
     * Not nullable, as every resource must have a creation date
     * Automatically set to the current date and time when the resource is created
     */
    @NotNull
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
