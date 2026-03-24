package org.elearning.backend.content.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "lessons")
public class Lesson {

    @Id
    @Column(name="id")
    @GeneratedValue
    private UUID id;

    //The commented lines can be uncommented once the Chapter classes are done
    //@ManyToOne
    //@JoinColumn(name = "chapter_id", nullable = false)
    //private Chapter chapterID;
    @Column(name="chapter_id", nullable = false)
    private UUID chapterID;

    @NotNull
    @Column(name="title")
    private String title;

    @Column(name="content_md")
    private String contentMarkdown;

    @NotNull
    @Column(name="order_index")
    private int orderIndex = 0;

    @NotNull
    @Column(name="created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @NotNull
    @Column(name="updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate(){
        this.updatedAt = LocalDateTime.now();
    }


}
