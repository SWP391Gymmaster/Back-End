package com.swp391_be.entity;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class GymClassValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void capacityIsRequired() {
        GymClass gymClass = new GymClass("Yoga", "Yoga", "Desc", null, 3L);

        assertThat(validator.validate(gymClass))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("capacity"));
    }

    @Test
    void classCodeValidationRejectsLowerCaseAndInvalidCharacters() {
        GymClass lowercaseCode = new GymClass("cls-yoga-01", "Yoga", "Yoga", "Desc", 30, 3L);
        assertThat(validator.validate(lowercaseCode))
                .anyMatch(v -> v.getPropertyPath().toString().equals("classCode"));

        GymClass shortCode = new GymClass("AB", "Yoga", "Yoga", "Desc", 30, 3L);
        assertThat(validator.validate(shortCode))
                .anyMatch(v -> v.getPropertyPath().toString().equals("classCode"));

        GymClass validCode = new GymClass("CLS-YOGA-01", "Yoga", "Yoga", "Desc", 30, 3L);
        assertThat(validator.validate(validCode)).isEmpty();
    }
}
