package org.elearning.backend.content.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;


@RestController
public class TestContent {

    private final JdbcTemplate jdbcTemplate;

    public TestContent(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/testAPI")
    public List<String> getTables() {
        String sql = """
            SELECT table_name 
            FROM information_schema.tables 
            WHERE table_schema = 'public'
            ORDER BY table_name
        """;
        return jdbcTemplate.queryForList(sql, String.class);
    }
}