package pl.corpai.azure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import lombok.extern.slf4j.Slf4j;
import pl.corpai.azure.client.BlobStorageClient;
import pl.corpai.azure.collector.WebScraper;
import pl.corpai.azure.dto.ScrapedData;
import pl.corpai.azure.dto.SanitizedCompanyPayload;
import pl.corpai.azure.llm.AzureOpenAiClient;
import pl.corpai.azure.llm.PromptBuilder;
import pl.corpai.azure.report.PdfReportGenerator;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class GenerateReportFunction {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebScraper webScraper = new WebScraper();
    private final PromptBuilder promptBuilder = new PromptBuilder();
    private final PdfReportGenerator pdfReportGenerator = new PdfReportGenerator();

    @FunctionName("generate-report")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    authLevel = AuthorizationLevel.FUNCTION)
            HttpRequestMessage<Optional<String>> request,
            ExecutionContext context) {

        log.info("Azure Function 'generate-report' wywołana");
        context.getLogger().info("Azure Function 'generate-report' wywołana");

        try {
            // 1. Deserialize body
            String body = request.getBody().orElse("");
            if (body.isEmpty()) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("{\"error\": \"Puste ciało żądania\"}")
                        .header("Content-Type", "application/json")
                        .build();
            }

            SanitizedCompanyPayload payload = objectMapper.readValue(body, SanitizedCompanyPayload.class);
            log.info("Przetwarzam raport dla spółki: {}", payload.getCompanyName());

            // 2. Scrape web data
            ScrapedData scrapedData = webScraper.scrape(payload);

            // 3. Build prompt
            String prompt = promptBuilder.build(payload, scrapedData);

            // 4. Call GPT-4o
            String openAiEndpoint = System.getenv("AZURE_OPENAI_ENDPOINT");
            String openAiKey = System.getenv("AZURE_OPENAI_KEY");
            String deployment = Optional.ofNullable(System.getenv("AZURE_OPENAI_DEPLOYMENT")).orElse("gpt-4o");

            AzureOpenAiClient openAiClient = new AzureOpenAiClient(openAiEndpoint, openAiKey, deployment);
            String narrative = openAiClient.generate(prompt);

            // 5. Generate PDF
            byte[] pdfBytes = pdfReportGenerator.generate(payload, scrapedData, narrative);

            // 6. Upload to Blob Storage
            String connectionString = System.getenv("AZURE_STORAGE_CONNECTION_STRING");
            String containerName = Optional.ofNullable(System.getenv("AZURE_STORAGE_CONTAINER")).orElse("reports");

            BlobStorageClient blobStorageClient = new BlobStorageClient(connectionString, containerName);
            String blobName = "report-" + payload.getKrsNumber() + "-" + Instant.now().getEpochSecond() + ".pdf";
            String blobUrl = blobStorageClient.upload(pdfBytes, blobName);

            // 7. Return response
            Map<String, String> response = new HashMap<>();
            response.put("blobUrl", blobUrl);
            response.put("status", "COMPLETED");

            return request.createResponseBuilder(HttpStatus.OK)
                    .body(objectMapper.writeValueAsString(response))
                    .header("Content-Type", "application/json")
                    .build();

        } catch (Exception e) {
            log.error("Błąd podczas generowania raportu: {}", e.getMessage(), e);
            context.getLogger().severe("Błąd: " + e.getMessage());
            try {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", e.getMessage());
                return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(objectMapper.writeValueAsString(errorResponse))
                        .header("Content-Type", "application/json")
                        .build();
            } catch (Exception jsonEx) {
                return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("{\"error\":\"Internal server error\"}")
                        .header("Content-Type", "application/json")
                        .build();
            }
        }
    }
}
