package com.asp.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;

@SpringBootApplication(exclude = {ReactiveWebServerFactoryAutoConfiguration.class})
public class AspWorkerApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(AspWorkerApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }
}

