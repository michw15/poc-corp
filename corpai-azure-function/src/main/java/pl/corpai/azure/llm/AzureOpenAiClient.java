package pl.corpai.azure.llm;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.*;
import com.azure.core.credential.AzureKeyCredential;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class AzureOpenAiClient {

    private final OpenAIClient client;
    private final String deployment;

    public AzureOpenAiClient(String endpoint, String key, String deployment) {
        this.client = new OpenAIClientBuilder()
                .endpoint(endpoint)
                .credential(new AzureKeyCredential(key))
                .buildClient();
        this.deployment = deployment;
    }

    public String generate(String prompt) {
        try {
            log.info("Wywołanie Azure OpenAI GPT-4o...");

            ChatCompletionsOptions options = new ChatCompletionsOptions(
                    List.of(new ChatRequestUserMessage(prompt))
            );
            options.setTemperature(0.3);
            options.setMaxTokens(800);

            ChatCompletions completions = client.getChatCompletions(deployment, options);

            String result = completions.getChoices().get(0).getMessage().getContent();
            log.info("Otrzymano odpowiedź z GPT-4o ({} znaków)", result.length());
            return result;

        } catch (Exception e) {
            log.error("Błąd wywołania Azure OpenAI: {}", e.getMessage());
            throw new RuntimeException("Błąd generowania narracji przez GPT-4o: " + e.getMessage(), e);
        }
    }
}
