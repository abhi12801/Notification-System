package com.notification.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NotificationSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationSystemApplication.class, args);
    }
}
