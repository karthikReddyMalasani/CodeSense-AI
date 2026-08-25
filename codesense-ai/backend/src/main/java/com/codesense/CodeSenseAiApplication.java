package com.codesense;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CodeSenseAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(CodeSenseAiApplication.class, args);
    }
}
