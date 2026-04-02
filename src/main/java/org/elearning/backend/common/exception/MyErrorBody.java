package org.elearning.backend.common.exception;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class MyErrorBody {

    private int status;
    private String message;

}