package org.elearning.backend.classroom.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.elearning.backend.classroom.entity.MembershipType;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class ClassroomMemberResponse {

    private UUID userId;
    private String email;
    private MembershipType membershipType;

}
