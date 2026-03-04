package pl.corpai.azure.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScrapedData {
    private List<NewsArticle> articles;
    private List<String> tenders;
    private String websiteAbout;
}
