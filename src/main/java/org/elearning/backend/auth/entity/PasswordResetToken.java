package org.elearning.backend.auth.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pw_reset_token")
public class PasswordResetToken extends BaseToken {
}
