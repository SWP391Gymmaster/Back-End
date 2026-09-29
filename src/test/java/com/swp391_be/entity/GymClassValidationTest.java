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
}
