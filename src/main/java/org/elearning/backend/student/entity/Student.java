package org.elearning.backend.student.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import lombok.Getter;
import lombok.Setter;
import org.elearning.backend.parent.entity.Parent;
import org.elearning.backend.user.entity.User;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@DiscriminatorValue("STUDENT")
public class Student extends User {
    @ManyToMany(mappedBy = "students")
    @JsonIgnore
    private Set<Parent> parents = new HashSet<>();
}
