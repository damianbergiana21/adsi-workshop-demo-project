package com.example.attendance.attendance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ClockOutRequest(
    @NotNull UUID employeeId,
    @Size(max = 200, message = "200文字以内で入力してください") String memo
) {}
