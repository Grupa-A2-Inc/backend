package org.elearning.backend.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class PaginatedResponse<T> {

    private List<T> content; //aici vreau sa fac raspuns generic
    // sa poata fi folosit pentru toate astea unde trb paginare
    //deci am pus T acolo sper ca e bine
    private Integer page;
    private Integer size;
    private Long totalElements;

}
