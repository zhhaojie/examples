package dev.springai.example;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.DefaultChatClientBuilder;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Scanner;

@Component
public class RunInCommandLine {

    @Resource
    private ChatClient chatClient;

    @EventListener(ApplicationReadyEvent.class)
    public void run() {
        Scanner scanner = new Scanner(System.in);
        String sessionId = "default-session";

        System.out.println("Customer Support Agent is running. Type 'exit' to quit.");

        while (true) {
            System.out.print("You: ");
            String userMessage = scanner.nextLine();

            if (userMessage.equalsIgnoreCase("exit")) {
                break;
            }

            if (!StringUtils.hasLength(userMessage)) {
                System.out.println("I can help you , please type something else.");
                continue;
            }

            String content = chatClient
                    .prompt(new Prompt(userMessage))
                    .call()
                    .content();
            System.out.println("AI:" + content);
        }

        System.out.println("Goodbye!");
        scanner.close();
    }

}
