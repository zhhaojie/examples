package dev.langchain4j.example;

import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.service.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Scanner;

@Slf4j
@Component
public class CustomerSupportAgentCMD {

    private final CustomerSupportAgent customerSupportAgent;

    public CustomerSupportAgentCMD(CustomerSupportAgent customerSupportAgent) {
        this.customerSupportAgent = customerSupportAgent;
    }

    public void start() {
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

            Result<String> result = customerSupportAgent.answer(sessionId, userMessage);
            System.out.print("AI: ");
            System.out.println(result.content());
        }

        System.out.println("Goodbye!");
        scanner.close();
    }

}
