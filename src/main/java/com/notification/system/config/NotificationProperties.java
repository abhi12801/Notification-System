package com.notification.system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized business-rule thresholds. Kept here instead of scattered magic
 * numbers (or a static "constants" class) so the rules are visible in one
 * place and tunable per environment without a code change.
 */
@ConfigurationProperties(prefix = "notification.rules")
public class NotificationProperties {

    /** Same user + type + message within this many minutes is rejected as a duplicate. */
    private int duplicateWindowMinutes = 5;

    /** A retry is only allowed once this many minutes have passed since the last attempt. */
    private int retryCooldownMinutes = 2;

    /** A notification cannot be retried once retryCount reaches this value. */
    private int maxRetryCount = 3;

    /** Approximate fraction of processed notifications the simulator randomly fails. */
    private double randomFailureRate = 0.30;

    public int getDuplicateWindowMinutes() {
        return duplicateWindowMinutes;
    }

    public void setDuplicateWindowMinutes(int duplicateWindowMinutes) {
        this.duplicateWindowMinutes = duplicateWindowMinutes;
    }

    public int getRetryCooldownMinutes() {
        return retryCooldownMinutes;
    }

    public void setRetryCooldownMinutes(int retryCooldownMinutes) {
        this.retryCooldownMinutes = retryCooldownMinutes;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public double getRandomFailureRate() {
        return randomFailureRate;
    }

    public void setRandomFailureRate(double randomFailureRate) {
        this.randomFailureRate = randomFailureRate;
    }
}
