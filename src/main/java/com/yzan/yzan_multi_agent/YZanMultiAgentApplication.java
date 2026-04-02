package com.yzan.yzan_multi_agent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.yzan.yzan_multi_agent.persistence.mapper")
@SpringBootApplication
public class YZanMultiAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(YZanMultiAgentApplication.class, args);
    }

}
