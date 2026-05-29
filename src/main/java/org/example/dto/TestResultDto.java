package org.example.dto;

public record TestResultDto(int testNumber, boolean passed, String input, String expected, String actual, boolean sample) {}
