package com.nr.mq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MessageQueueServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessageQueueServerApplication.class, args);
    }

}
