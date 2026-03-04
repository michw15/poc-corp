package pl.corpai.azure.report;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import pl.corpai.azure.dto.NewsArticle;
import pl.corpai.azure.dto.ScrapedData;
import pl.corpai.azure.dto.SanitizedCompanyPayload;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
public class PdfReportGenerator {

    private static final DeviceRgb HEADER_COLOR = new DeviceRgb(0, 51, 102);
    private static final DeviceRgb TEXT_COLOR = new DeviceRgb(51, 51, 51);
    private static final DeviceRgb SECTION_BG = new DeviceRgb(240, 244, 248);

    public byte[] generate(SanitizedCompanyPayload payload, ScrapedData scrapedData, String narrative) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);

            addHeader(document, payload);
            addRegistrationData(document, payload);
            addNews(document, scrapedData);
            addTenders(document, scrapedData);
            addAiAnalysis(document, narrative);

            document.close();
            log.info("PDF wygenerowany ({} bajtów)", outputStream.size());
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Błąd generowania PDF: {}", e.getMessage());
            throw new RuntimeException("Błąd generowania PDF: " + e.getMessage(), e);
        }
    }

    private void addHeader(Document document, SanitizedCompanyPayload payload) {
        Paragraph title = new Paragraph("CORP AI")
                .setFontColor(HEADER_COLOR)
                .setBold()
                .setFontSize(24)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(title);

        Paragraph subtitle = new Paragraph("Raport Spółki Korporacyjnej")
                .setFontColor(HEADER_COLOR)
                .setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(subtitle);

        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        Paragraph generatedAt = new Paragraph("Wygenerowano: " + dateTime)
                .setFontColor(TEXT_COLOR)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(generatedAt);

        document.add(new Paragraph("\n"));
    }

    private void addRegistrationData(Document document, SanitizedCompanyPayload payload) {
        Paragraph sectionHeader = new Paragraph("DANE REJESTROWE")
                .setFontColor(HEADER_COLOR)
                .setBold()
                .setFontSize(13)
                .setBackgroundColor(SECTION_BG);
        document.add(sectionHeader);

        Table table = new Table(UnitValue.createPercentArray(new float[]{2, 3}))
                .useAllAvailableWidth();

        addTableRow(table, "Nazwa", payload.getCompanyName());
        addTableRow(table, "NIP", payload.getNip());
        addTableRow(table, "KRS", payload.getKrsNumber());
        addTableRow(table, "PKD główne", payload.getPkdMain());
        addTableRow(table, "Siedziba", payload.getCity());
        addTableRow(table, "Data rejestracji", payload.getRegistrationDate());
        if (payload.getShareCapital() != null) {
            addTableRow(table, "Kapitał", payload.getShareCapital().toPlainString() + " PLN");
        }

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addTableRow(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label).setBold().setFontColor(HEADER_COLOR)));
        table.addCell(new Cell().add(new Paragraph(value != null ? value : "—").setFontColor(TEXT_COLOR)));
    }

    private void addNews(Document document, ScrapedData scrapedData) {
        Paragraph sectionHeader = new Paragraph("AKTUALNOŚCI")
                .setFontColor(HEADER_COLOR)
                .setBold()
                .setFontSize(13)
                .setBackgroundColor(SECTION_BG);
        document.add(sectionHeader);

        List<NewsArticle> articles = scrapedData.getArticles();
        if (articles != null && !articles.isEmpty()) {
            for (NewsArticle article : articles) {
                StringBuilder text = new StringBuilder("• ").append(article.getTitle());
                if (article.getSnippet() != null && !article.getSnippet().isEmpty()) {
                    text.append("\n  ").append(article.getSnippet());
                }
                document.add(new Paragraph(text.toString()).setFontColor(TEXT_COLOR).setFontSize(10));
            }
        } else {
            document.add(new Paragraph("Brak dostępnych aktualności.").setFontColor(TEXT_COLOR).setItalic());
        }
        document.add(new Paragraph("\n"));
    }

    private void addTenders(Document document, ScrapedData scrapedData) {
        Paragraph sectionHeader = new Paragraph("AKTYWNOŚĆ PRZETARGOWA")
                .setFontColor(HEADER_COLOR)
                .setBold()
                .setFontSize(13)
                .setBackgroundColor(SECTION_BG);
        document.add(sectionHeader);

        List<String> tenders = scrapedData.getTenders();
        if (tenders != null && !tenders.isEmpty()) {
            for (String tender : tenders) {
                document.add(new Paragraph("• " + tender).setFontColor(TEXT_COLOR).setFontSize(10));
            }
        } else {
            document.add(new Paragraph("Brak danych.").setFontColor(TEXT_COLOR).setItalic());
        }
        document.add(new Paragraph("\n"));
    }

    private void addAiAnalysis(Document document, String narrative) {
        Paragraph sectionHeader = new Paragraph("ANALIZA AI")
                .setFontColor(HEADER_COLOR)
                .setBold()
                .setFontSize(13)
                .setBackgroundColor(SECTION_BG);
        document.add(sectionHeader);

        document.add(new Paragraph(narrative != null ? narrative : "Brak analizy.")
                .setFontColor(TEXT_COLOR)
                .setFontSize(10));
    }
}
