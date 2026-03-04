package pl.corpai.orchestrator.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import pl.corpai.common.dto.AzureReportResponse;
import pl.corpai.common.dto.SanitizedCompanyPayload;

@Slf4j
@Component
@RequiredArgsConstructor
public class AzureFunctionClient {

    @Value("${azure.function.url}")
    private String azureFunctionUrl;

    @Value("${azure.function.key}")
    private String azureFunctionKey;

    private final RestTemplate restTemplate;

    @Retryable(retryFor = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public AzureReportResponse generateReport(SanitizedCompanyPayload payload) {
        log.info("Wysyłanie żądania do Azure Function dla spółki: {}", payload.getCompanyName());
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-functions-key", azureFunctionKey);

            HttpEntity<SanitizedCompanyPayload> request = new HttpEntity<>(payload, headers);
            String url = azureFunctionUrl + "/api/generate-report";

            ResponseEntity<AzureReportResponse> response = restTemplate.postForEntity(
                    url, request, AzureReportResponse.class);

            log.info("Otrzymano odpowiedź z Azure Function: {}", response.getStatusCode());
            return response.getBody();

        } catch (Exception e) {
            log.error("Błąd wywołania Azure Function: {}", e.getMessage());
            throw new RuntimeException("Błąd generowania raportu przez Azure Function: " + e.getMessage(), e);
        }
    }
}
