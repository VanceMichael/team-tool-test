package com.standup;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.standup.mapper")
public class StandupApplication {
    public static void main(String[] args) {
        SpringApplication.run(StandupApplication.class, args);
    }
}
