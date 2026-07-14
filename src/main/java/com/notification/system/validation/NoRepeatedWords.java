package com.notification.system.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rejects a message containing any word repeated more than {@link #maxConsecutive()}
 * times in a row (e.g. "hello hello hello hello" is invalid; "hello hello hello" is valid).
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = NoRepeatedWordsValidator.class)
public @interface NoRepeatedWords {

    String message() default "message must not contain a word repeated more than {maxConsecutive} times consecutively";

    int maxConsecutive() default 3;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
