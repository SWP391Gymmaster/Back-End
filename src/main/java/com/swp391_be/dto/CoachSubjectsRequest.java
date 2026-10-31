package com.swp391_be.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CoachSubjectsRequest(@NotNull List<String> subjects) {
}
