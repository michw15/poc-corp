package pl.corpai.azure.collector;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import pl.corpai.azure.dto.NewsArticle;
import pl.corpai.azure.dto.ScrapedData;
import pl.corpai.azure.dto.SanitizedCompanyPayload;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class WebScraper {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    private static final int TIMEOUT_MS = 10000;

    public ScrapedData scrape(SanitizedCompanyPayload payload) {
        String searchQuery = payload.getScrapingHints() != null ?
                payload.getScrapingHints().getSearchQuery() : payload.getCompanyName();
        String website = payload.getScrapingHints() != null ?
                payload.getScrapingHints().getWebsite() : null;

        List<NewsArticle> articles = scrapeNews(searchQuery);
        List<String> tenders = scrapeTenders(payload.getCompanyName());
        String aboutText = scrapeWebsite(website);

        return new ScrapedData(articles, tenders, aboutText);
    }

    private List<NewsArticle> scrapeNews(String query) {
        List<NewsArticle> articles = new ArrayList<>();
        try {
            String url = "https://www.bing.com/search?q=" +
                    java.net.URLEncoder.encode(query + " news", java.nio.charset.StandardCharsets.UTF_8) +
                    "&setlang=pl";
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .get();

            Elements results = doc.select("li.b_algo");
            int count = 0;
            for (Element result : results) {
                if (count >= 5) break;
                String title = result.select("h2").text();
                String snippet = result.select("p").text();
                String articleUrl = result.select("h2 a").attr("href");
                if (!title.isEmpty()) {
                    articles.add(new NewsArticle(title, snippet, articleUrl));
                    count++;
                }
            }
        } catch (Exception e) {
            log.warn("Błąd scrapowania wiadomości: {}", e.getMessage());
        }
        return articles;
    }

    private List<String> scrapeTenders(String companyName) {
        List<String> tenders = new ArrayList<>();
        try {
            String url = "https://ted.europa.eu/en/search?q=" +
                    java.net.URLEncoder.encode(companyName, java.nio.charset.StandardCharsets.UTF_8);
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .get();

            Elements results = doc.select(".ted-result-title, h3.result-title, .result h3");
            int count = 0;
            for (Element result : results) {
                if (count >= 3) break;
                String title = result.text();
                if (!title.isEmpty()) {
                    tenders.add(title);
                    count++;
                }
            }
        } catch (Exception e) {
            log.warn("Błąd scrapowania przetargów: {}", e.getMessage());
        }
        return tenders;
    }

    private String scrapeWebsite(String website) {
        if (website == null || website.trim().isEmpty()) {
            return "";
        }
        try {
            String url = website.startsWith("http") ? website : "https://" + website;
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .get();

            // Try meta description first
            Element metaDesc = doc.select("meta[name=description]").first();
            if (metaDesc != null && !metaDesc.attr("content").isEmpty()) {
                return metaDesc.attr("content");
            }

            // Fall back to first 500 chars of body text
            String bodyText = doc.body().text();
            return bodyText.length() > 500 ? bodyText.substring(0, 500) : bodyText;

        } catch (Exception e) {
            log.warn("Błąd scrapowania strony internetowej {}: {}", website, e.getMessage());
            return "";
        }
    }
}
