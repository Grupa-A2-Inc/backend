package org.elearning.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.function.BiFunction;

@SpringBootApplication
@EnableScheduling
public class BackendApplication {

    static BiFunction<Class<?>, String[], ConfigurableApplicationContext> applicationRunner = SpringApplication::run;

    public static void main(String[] args) {
        applicationRunner.apply(BackendApplication.class, args);

    }

}
