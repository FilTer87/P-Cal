package com.privatecal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PrivateCalApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrivateCalApplication.class, args);
    }
}