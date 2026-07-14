package com.notification.system.util;

import com.notification.system.config.NotificationProperties;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Stands in for a real external channel's occasional failures, so the retry
 * pipeline and status transitions have something real to react to. Not a
 * security-sensitive use of randomness, so ThreadLocalRandom is sufficient.
 */
@Component
public class RandomFailureSimulator {

    private final double failureRate;

    public RandomFailureSimulator(NotificationProperties properties) {
        this.failureRate = properties.getRandomFailureRate();
    }

    public boolean shouldFail() {
        return ThreadLocalRandom.current().nextDouble() < failureRate;
    }
}
