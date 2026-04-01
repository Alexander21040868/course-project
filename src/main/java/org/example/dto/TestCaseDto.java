package org.example.dto;

public record TestCaseDto(Long id, String input, String expectedOutput, boolean sample) {}
