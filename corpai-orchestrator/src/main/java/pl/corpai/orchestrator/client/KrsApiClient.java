package pl.corpai.orchestrator.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import pl.corpai.common.dto.BoardMember;
import pl.corpai.common.dto.KrsData;
import pl.corpai.orchestrator.exception.KrsNotFoundException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KrsApiClient {

    private static final String KRS_API_URL = "https://api.rejestry.ms.gov.pl/api/krs/podmioty/podmiotHandlowyPelnyJawny/{krs}";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public KrsData fetch(String krs) {
        log.info("Pobieranie danych KRS dla: {}", krs);
        try {
            String response = restTemplate.getForObject(KRS_API_URL, String.class, krs);
            return parseKrsResponse(krs, response);
        } catch (HttpClientErrorException.NotFound e) {
            throw new KrsNotFoundException(krs);
        } catch (Exception e) {
            log.error("Błąd podczas pobierania danych KRS dla {}: {}", krs, e.getMessage());
            throw new RuntimeException("Błąd komunikacji z KRS API: " + e.getMessage(), e);
        }
    }

    private KrsData parseKrsResponse(String krs, String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode odpis = root.path("odpis");
            JsonNode dane = odpis.path("dane");
            JsonNode podmiot = dane.path("podmiot");

            String name = podmiot.path("nazwaPodmiotu").asText("");
            String nip = podmiot.path("nip").asText("");
            String regon = podmiot.path("regon").asText("");
            String legalForm = podmiot.path("formaWlasnosci").path("nazwaPelna").asText("");
            String city = podmiot.path("siedzibaIAdresPodmiotu").path("adresSiedziby").path("miejscowosc").asText("");
            String voivodeship = podmiot.path("siedzibaIAdresPodmiotu").path("adresSiedziby").path("wojewodztwo").asText("");
            String registrationDate = podmiot.path("dataRejestracjiWKRS").asText("");
            BigDecimal shareCapital = null;
            try {
                String capitalStr = podmiot.path("kapitalSpolki").path("wysokoscKapitaluZakladowego").asText("");
                if (!capitalStr.isEmpty()) {
                    shareCapital = new BigDecimal(capitalStr.replace(",", "."));
                }
            } catch (Exception ignored) {}

            String pkdMain = "";
            List<String> pkdCodes = new ArrayList<>();
            JsonNode pkdNode = podmiot.path("przedmiotDzialalnosci");
            if (!pkdNode.isMissingNode()) {
                JsonNode przewazajace = pkdNode.path("przedmiotPrzewajajacej");
                if (!przewazajace.isMissingNode() && przewazajace.isArray() && przewazajace.size() > 0) {
                    pkdMain = przewazajace.get(0).path("kodDzial").asText("") + " " +
                              przewazajace.get(0).path("opis").asText("");
                }
                JsonNode pozostale = pkdNode.path("przedmiotPozostalej");
                if (!pozostale.isMissingNode() && pozostale.isArray()) {
                    for (JsonNode pkd : pozostale) {
                        pkdCodes.add(pkd.path("kodDzial").asText("") + " " + pkd.path("opis").asText(""));
                    }
                }
            }

            String website = "";
            JsonNode kontakt = podmiot.path("daneKontaktowe");
            if (!kontakt.isMissingNode()) {
                website = kontakt.path("adresStronyInternetowej").asText("");
            }

            List<BoardMember> board = new ArrayList<>();
            JsonNode reprezentacja = dane.path("organRejestrowy").path("reprezentacja");
            if (!reprezentacja.isMissingNode() && reprezentacja.isArray()) {
                for (JsonNode member : reprezentacja) {
                    String role = member.path("funkcja").asText("");
                    String firstName = member.path("imie1").asText("");
                    String lastName = member.path("nazwisko").asText("");
                    board.add(new BoardMember(role, firstName + " " + lastName));
                }
            }

            return KrsData.builder()
                    .krs(krs)
                    .nip(nip)
                    .regon(regon)
                    .name(name)
                    .legalForm(legalForm)
                    .pkdMain(pkdMain.trim())
                    .pkdCodes(pkdCodes)
                    .city(city)
                    .voivodeship(voivodeship)
                    .registrationDate(registrationDate)
                    .shareCapital(shareCapital)
                    .board(board)
                    .website(website)
                    .build();

        } catch (Exception e) {
            log.error("Błąd parsowania odpowiedzi KRS: {}", e.getMessage());
            throw new RuntimeException("Błąd parsowania odpowiedzi KRS API", e);
        }
    }
}
