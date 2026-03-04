package pl.corpai.azure.llm;

import pl.corpai.azure.dto.NewsArticle;
import pl.corpai.azure.dto.ScrapedData;
import pl.corpai.azure.dto.SanitizedCompanyPayload;

import java.util.List;

public class PromptBuilder {

    public String build(SanitizedCompanyPayload payload, ScrapedData scrapedData) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Jesteś analitykiem bankowym. Na podstawie poniższych danych przygotuj profesjonalny raport analizy spółki.\n\n");
        prompt.append("INSTRUKCJE:\n");
        prompt.append("- Maksymalnie 400 słów\n");
        prompt.append("- Profesjonalny ton bankowy\n");
        prompt.append("- 4 sekcje: 1) Profil spółki, 2) Ocena działalności, 3) Aktualności i ryzyka, 4) Podsumowanie\n\n");

        // Dane rejestrowe
        prompt.append("=== DANE REJESTROWE ===\n");
        prompt.append("Nazwa: ").append(payload.getCompanyName()).append("\n");
        prompt.append("NIP: ").append(payload.getNip()).append("\n");
        prompt.append("KRS: ").append(payload.getKrsNumber()).append("\n");
        if (payload.getPkdMain() != null) {
            prompt.append("Główna działalność (PKD): ").append(payload.getPkdMain()).append("\n");
        }
        if (payload.getCity() != null) {
            prompt.append("Siedziba: ").append(payload.getCity());
            if (payload.getVoivodeship() != null) {
                prompt.append(", woj. ").append(payload.getVoivodeship());
            }
            prompt.append("\n");
        }
        if (payload.getRegistrationDate() != null) {
            prompt.append("Data rejestracji: ").append(payload.getRegistrationDate()).append("\n");
        }
        if (payload.getShareCapital() != null) {
            prompt.append("Kapitał zakładowy: ").append(payload.getShareCapital()).append(" PLN\n");
        }
        if (payload.getBoardInitials() != null && !payload.getBoardInitials().isEmpty()) {
            prompt.append("Zarząd (inicjały): ").append(String.join(", ", payload.getBoardInitials())).append("\n");
        }

        // Aktualności
        prompt.append("\n=== AKTUALNOŚCI ===\n");
        List<NewsArticle> articles = scrapedData.getArticles();
        if (articles != null && !articles.isEmpty()) {
            for (NewsArticle article : articles) {
                prompt.append("- ").append(article.getTitle());
                if (article.getSnippet() != null && !article.getSnippet().isEmpty()) {
                    prompt.append(": ").append(article.getSnippet());
                }
                prompt.append("\n");
            }
        } else {
            prompt.append("Brak dostępnych aktualności.\n");
        }

        // Aktywność przetargowa
        prompt.append("\n=== AKTYWNOŚĆ PRZETARGOWA ===\n");
        List<String> tenders = scrapedData.getTenders();
        if (tenders != null && !tenders.isEmpty()) {
            for (String tender : tenders) {
                prompt.append("- ").append(tender).append("\n");
            }
        } else {
            prompt.append("Brak danych o przetargach.\n");
        }

        // O firmie
        if (scrapedData.getWebsiteAbout() != null && !scrapedData.getWebsiteAbout().isEmpty()) {
            prompt.append("\n=== O FIRMIE (ze strony) ===\n");
            prompt.append(scrapedData.getWebsiteAbout()).append("\n");
        }

        prompt.append("\nPrzygotuj raport analizy:");

        return prompt.toString();
    }
}
