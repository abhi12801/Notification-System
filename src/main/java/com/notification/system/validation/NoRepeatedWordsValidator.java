package com.notification.system.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Scans word-by-word rather than using a backreference regex
 * (e.g. {@code (\w+)(\s+\1){3,}}) — a hand-rolled scan is easier to read,
 * easier to unit test in isolation, and avoids catastrophic-backtracking
 * risk on adversarial input.
 */
public class NoRepeatedWordsValidator implements ConstraintValidator<NoRepeatedWords, String> {

    private int maxConsecutive;

    @Override
    public void initialize(NoRepeatedWords annotation) {
        this.maxConsecutive = annotation.maxConsecutive();
    }

    @Override
    public boolean isValid(String message, ConstraintValidatorContext context) {
        if (message == null || message.isBlank()) {
            return true; // absence/blankness is @NotBlank's concern, not this validator's
        }

        String[] words = message.trim().split("\\s+");
        int consecutiveCount = 1;

        for (int i = 1; i < words.length; i++) {
            boolean sameAsPrevious = words[i].equalsIgnoreCase(words[i - 1]);
            consecutiveCount = sameAsPrevious ? consecutiveCount + 1 : 1;
            if (consecutiveCount > maxConsecutive) {
                return false;
            }
        }
        return true;
    }
}
