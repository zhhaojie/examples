package com.example.springresourceioexample;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.PathResource;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@SpringBootApplication
public class SpringResourceIoExampleApplication implements CommandLineRunner {

    @Resource
    ApplicationContext applicationContext;

    public static void main(String[] args) throws IOException {
        SpringApplication.run(SpringResourceIoExampleApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        readFile();
    }

    public void readFile() throws IOException {
        ClassPathResource resource = (ClassPathResource) applicationContext.getResource("classpath:word.txt");
        InputStreamReader read = new InputStreamReader(resource.getInputStream());
        BufferedReader bufferedReader = new BufferedReader(read);
        bufferedReader.lines().forEach(x -> System.out.println(x));
    }
}
