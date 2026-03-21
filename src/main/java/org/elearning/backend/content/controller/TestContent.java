package org.elearning.backend.content.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestContent {

    private final JdbcTemplate jdbcTemplate;

    public TestContent(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/testAPI")
    public String spuneSalut() {
        String time = jdbcTemplate.queryForObject("SELECT NOW()", String.class);
        return "Salut! Timpul din baza de date este: " + time;
    }
}