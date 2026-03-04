package pl.corpai.report.service;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;
import pl.corpai.common.dto.CrbrBeneficiary;
import pl.corpai.common.dto.CrbrData;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class CrbrSectionAppender {

    private static final DeviceRgb HEADER_COLOR = new DeviceRgb(0, 51, 102);
    private static final DeviceRgb TEXT_COLOR = new DeviceRgb(51, 51, 51);

    public byte[] append(byte[] existingPdfBytes, CrbrData crbrData) {
        if (crbrData == null || crbrData.getBeneficiaries() == null || crbrData.getBeneficiaries().isEmpty()) {
            return existingPdfBytes;
        }

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfReader reader = new PdfReader(new ByteArrayInputStream(existingPdfBytes));
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDocument = new PdfDocument(reader, writer);
            Document document = new Document(pdfDocument);

            // Add new page
            pdfDocument.addNewPage();

            // Section header
            Paragraph header = new Paragraph("BENEFICJENCI RZECZYWIŚCI (CRBR)")
                    .setFontColor(HEADER_COLOR)
                    .setBold()
                    .setFontSize(14);
            document.add(header);

            // Table with beneficiaries
            Table table = new Table(UnitValue.createPercentArray(new float[]{3, 2, 3}))
                    .useAllAvailableWidth();

            // Header row
            table.addHeaderCell(new Cell().add(new Paragraph("Imię i nazwisko").setBold().setFontColor(HEADER_COLOR)));
            table.addHeaderCell(new Cell().add(new Paragraph("Udział (%)").setBold().setFontColor(HEADER_COLOR)));
            table.addHeaderCell(new Cell().add(new Paragraph("Typ kontroli").setBold().setFontColor(HEADER_COLOR)));

            // Data rows
            List<CrbrBeneficiary> beneficiaries = crbrData.getBeneficiaries();
            for (CrbrBeneficiary b : beneficiaries) {
                table.addCell(new Cell().add(new Paragraph(b.getFirstName() + " " + b.getLastName()).setFontColor(TEXT_COLOR)));
                table.addCell(new Cell().add(new Paragraph(b.getSharePercent() != null ? b.getSharePercent().toString() : "—").setFontColor(TEXT_COLOR)));
                table.addCell(new Cell().add(new Paragraph(b.getControlType() != null ? b.getControlType() : "—").setFontColor(TEXT_COLOR)));
            }

            document.add(table);

            // Footer
            Paragraph footer = new Paragraph("Dane CRBR — źródło: Centralny Rejestr Beneficjentów Rzeczywistych")
                    .setFontSize(8)
                    .setFontColor(TEXT_COLOR)
                    .setItalic();
            document.add(footer);

            document.close();
            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Błąd podczas dodawania sekcji CRBR do PDF", e);
        }
    }
}
