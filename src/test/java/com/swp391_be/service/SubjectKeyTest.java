package com.swp391_be.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SubjectKeyTest {

    @Test
    void normalizesSubjectNames() {
        assertThat(SubjectKey.of("Yoga")).isEqualTo("YOGA");
        assertThat(SubjectKey.of(" group-x ")).isEqualTo("GROUP_X");
        assertThat(SubjectKey.of("GroupX")).isEqualTo("GROUP_X");
        assertThat(SubjectKey.of("Võ cổ truyền")).isEqualTo("VO_CO_TRUYEN");
        assertThat(SubjectKey.of(null)).isEmpty();
    }
}