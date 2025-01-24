package dev.langchain4j.example;

import dev.langchain4j.model.chat.ChatLanguageModel;
import jakarta.annotation.Resource;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class RunExample {

    @Resource
    ChatLanguageModel languageModel;

    @EventListener(ApplicationReadyEvent.class)
    public void runExample() {
    }
}
