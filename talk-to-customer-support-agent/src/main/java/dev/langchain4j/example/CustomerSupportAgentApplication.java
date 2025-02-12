package dev.langchain4j.example;

import jakarta.annotation.Resource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CustomerSupportAgentApplication implements CommandLineRunner {

    @Resource
    private CustomerSupportAgentStartup agentCMD;

    public static void main(String[] args) {
        SpringApplication.run(CustomerSupportAgentApplication.class, args);
    }

    @Override
    public void run(String... args) {
        agentCMD.start();
    }
}
