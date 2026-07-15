package com.hollysys.ppa;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.hollysys.ppa.module.*.*.mapper")
public class PpaApplication {
    public static void main(String[] args) {
        SpringApplication.run(PpaApplication.class, args);
    }
}
