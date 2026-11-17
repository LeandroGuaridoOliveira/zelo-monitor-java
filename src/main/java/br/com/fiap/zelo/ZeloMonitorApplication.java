package br.com.fiap.zelo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ZeloMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZeloMonitorApplication.class, args);
    }

}
