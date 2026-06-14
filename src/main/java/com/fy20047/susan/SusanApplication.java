package com.fy20047.susan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SusanApplication {

    public static void main(String[] args) {
        SpringApplication.run(SusanApplication.class, args);
    }

}
