package pl.corpai.orchestrator.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import pl.corpai.common.dto.CrbrBeneficiary;
import pl.corpai.common.dto.CrbrData;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrbrApiClient {

    private static final String CRBR_API_URL = "https://beneficial-owner.mf.gov.pl/api/beneficial-owner/company/{nip}";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public CrbrData fetch(String nip) {
        log.info("Pobieranie danych CRBR dla NIP: {}", nip);
        try {
            String response = restTemplate.getForObject(CRBR_API_URL, String.class, nip);
            return parseCrbrResponse(nip, response);
        } catch (Exception e) {
            log.warn("CRBR API niedostępne dla NIP {}: {}. Kontynuowanie bez danych CRBR.", nip, e.getMessage());
            return CrbrData.builder()
                    .nip(nip)
                    .beneficiaries(new ArrayList<>())
                    .build();
        }
    }

    private CrbrData parseCrbrResponse(String nip, String responseBody) {
        try {
            List<CrbrBeneficiary> beneficiaries = new ArrayList<>();
            JsonNode root = objectMapper.readTree(responseBody);

            JsonNode beneficiariesNode = root.path("beneficialOwners");
            if (!beneficiariesNode.isMissingNode() && beneficiariesNode.isArray()) {
                for (JsonNode node : beneficiariesNode) {
                    String firstName = node.path("firstName").asText("");
                    String lastName = node.path("lastName").asText("");
                    BigDecimal sharePercent = null;
                    try {
                        String shareStr = node.path("sharePercent").asText("");
                        if (!shareStr.isEmpty()) {
                            sharePercent = new BigDecimal(shareStr);
                        }
                    } catch (Exception ignored) {}
                    String controlType = node.path("controlType").asText("");
                    beneficiaries.add(new CrbrBeneficiary(firstName, lastName, sharePercent, controlType));
                }
            }

            return CrbrData.builder()
                    .nip(nip)
                    .beneficiaries(beneficiaries)
                    .build();

        } catch (Exception e) {
            log.warn("Błąd parsowania odpowiedzi CRBR: {}. Zwracam puste dane.", e.getMessage());
            return CrbrData.builder()
                    .nip(nip)
                    .beneficiaries(new ArrayList<>())
                    .build();
        }
    }
}
