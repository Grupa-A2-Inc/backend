package org.elearning.backend.parent.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.elearning.backend.student.entity.Student;
import org.elearning.backend.user.entity.User;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@DiscriminatorValue("PARENT")
public class Parent extends User {
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "parent_student",
            joinColumns = @JoinColumn(name = "id_parent"),
            inverseJoinColumns = @JoinColumn(name = "id_student")
    )
    @JsonIgnore
    private Set<Student> students = new HashSet<>();
}
