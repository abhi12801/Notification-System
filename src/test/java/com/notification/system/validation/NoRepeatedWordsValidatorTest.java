package com.notification.system.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NoRepeatedWordsValidatorTest {

    private NoRepeatedWordsValidator validator;

    @BeforeEach
    void setUp() {
        validator = new NoRepeatedWordsValidator();
        NoRepeatedWords annotation = Mockito.mock(NoRepeatedWords.class);
        Mockito.when(annotation.maxConsecutive()).thenReturn(3);
        validator.initialize(annotation);
    }

    @Test
    void rejectsWordRepeatedFourTimesConsecutively() {
        assertThat(validator.isValid("hello hello hello hello", context())).isFalse();
    }

    @Test
    void acceptsWordRepeatedExactlyThreeTimes() {
        assertThat(validator.isValid("hello hello hello", context())).isTrue();
    }

    @Test
    void acceptsOrdinaryMessage() {
        assertThat(validator.isValid("Welcome to the platform", context())).isTrue();
    }

    @Test
    void isCaseInsensitiveWhenCountingRepeats() {
        assertThat(validator.isValid("Hello hello HELLO hello", context())).isFalse();
    }

    @Test
    void resetsRunAfterADifferentWordBreaksIt() {
        assertThat(validator.isValid("hi hi hi there hi hi hi", context())).isTrue();
    }

    @Test
    void treatsNullAndBlankAsValid() {
        assertThat(validator.isValid(null, context())).isTrue();
        assertThat(validator.isValid("   ", context())).isTrue();
    }

    private ConstraintValidatorContext context() {
        return Mockito.mock(ConstraintValidatorContext.class);
    }
}
