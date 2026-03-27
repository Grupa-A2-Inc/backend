package org.elearning.backend.content.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "chapters")
@Getter
@Setter // Am schimbat @Data cu @Getter și @Setter pentru a preveni erori (StackOverflowError) la relații Lazy
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY) // Multe capitole la un singur curs
    @JoinColumn(name = "course_id", nullable = false)
    private Course course; // <-- REZOLVAREA 1: Schimbat din courseId în course!

    private String title;

    @Column(name="order_index")
    private int orderIndex;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // <-- REZOLVAREA 2: Am adăugat lista de lecții care lipsea complet!
    @OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Lesson> lessons = new ArrayList<>();
}