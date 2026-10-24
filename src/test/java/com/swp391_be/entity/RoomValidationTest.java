package com.swp391_be.entity;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class RoomValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void roomCodeValidationRejectsLowerCaseAndInvalidCharacters() {
        Room lowercaseCode = new Room("room-gym-a", "Phòng Gym A", 30, RoomType.GYM);
        assertThat(validator.validate(lowercaseCode))
                .anyMatch(v -> v.getPropertyPath().toString().equals("roomCode"));

        Room shortCode = new Room("R1", "Phòng Gym A", 30, RoomType.GYM);
        assertThat(validator.validate(shortCode))
                .anyMatch(v -> v.getPropertyPath().toString().equals("roomCode"));

        Room validCode = new Room("ROOM-GYM-A", "Phòng Gym A", 30, RoomType.GYM);
        assertThat(validator.validate(validCode)).isEmpty();
    }
}
