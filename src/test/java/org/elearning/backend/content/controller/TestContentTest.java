package org.elearning.backend.content.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestContentTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private TestContent testContent;

    @Test
    void getTables_returnsTableNamesFromJdbcTemplate() {
        List<String> tables = List.of("chapters", "courses");
        when(jdbcTemplate.queryForList(anyString(), eq(String.class))).thenReturn(tables);

        List<String> response = testContent.getTables();

        assertThat(response).isEqualTo(tables);
    }
}
