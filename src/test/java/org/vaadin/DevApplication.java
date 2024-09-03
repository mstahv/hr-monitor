package org.vaadin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The entry point of the Spring Boot application.
 */
@SpringBootApplication
public class DevApplication {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
