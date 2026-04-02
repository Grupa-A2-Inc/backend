package org.elearning.backend.assessment.mapper;

import org.elearning.backend.assessment.dto.TestEditDto;
import org.elearning.backend.assessment.dto.TestEntityDto;
import org.elearning.backend.assessment.model.Test;
import org.mapstruct.Mapper;

/**
 * Mapper interface for mapping between Test and its corresponding DTOs.
 */
@Mapper(componentModel = "spring")
public interface TestMapper {
    /**
     * Map the Test entity to a TestEntityDTO.
     * @param test The Test entity to be mapped to a TestEntityDto.
     * @return A TestEntityDto containing the mapped data from the Test entity.
     */
    TestEntityDto toEntityDto(Test test);


    /**
     * Map the TestCreateDto to a Test entity.
     * @param dto The TestCreateDto instance to be mapped to a Test entity.
     * @return A Test entity containing the mapped data from the TestCreateDto instance.
     */

    Test toEntity(TestEditDto dto);
}
