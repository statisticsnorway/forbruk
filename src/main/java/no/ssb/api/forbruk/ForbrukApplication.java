package no.ssb.api.forbruk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Created by rsa on 29.04.2019.
 */
@SpringBootApplication
@EnableScheduling
public class ForbrukApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ForbrukApplication.class);
        app.run();
    }}
