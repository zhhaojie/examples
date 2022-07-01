package com.example.redis;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;

@SpringBootApplication
public class Application implements CommandLineRunner {

    private final StringRedisTemplate stringRedisTemplate;
    @Resource
    ApplicationContext applicationContext;

    public Application(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        stringRedisTemplate.opsForValue().set("company", "www.apple.com");
        final String company = stringRedisTemplate.opsForValue().get("company");
        System.out.println(company);

        System.out.println(applicationContext.getBeanDefinitionCount());
        for (int i = 0; i < applicationContext.getBeanDefinitionNames().length; i++) {
            System.out.println(applicationContext.getBean((applicationContext.getBeanDefinitionNames()[i])));
        }

    }
}
