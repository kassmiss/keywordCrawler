package com.grlife.newkeyword;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class NewkeywordApplication {

    public static void main(String[] args) {
        SpringApplication.run(NewkeywordApplication.class, args);
    }

}
