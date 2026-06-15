package com.underpass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UnderpassFloodApplication {
    public static void main(String[] args) {
        SpringApplication.run(UnderpassFloodApplication.class, args);
    }
}
