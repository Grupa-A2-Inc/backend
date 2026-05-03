package org.elearning.backend.auth.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "activation_token")
public class ActivationToken extends BaseToken {
}
