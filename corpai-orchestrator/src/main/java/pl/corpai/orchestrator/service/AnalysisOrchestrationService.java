package pl.corpai.orchestrator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import pl.corpai.common.dto.*;
import pl.corpai.common.entity.AnalysisRequestEntity;
import pl.corpai.common.entity.ReportEntity;
import pl.corpai.common.enums.AnalysisStatus;
import pl.corpai.orchestrator.client.AzureFunctionClient;
import pl.corpai.orchestrator.client.CrbrApiClient;
import pl.corpai.orchestrator.client.KrsApiClient;
import pl.corpai.orchestrator.exception.KrsNotFoundException;
import pl.corpai.orchestrator.repository.AnalysisRequestRepository;
import pl.corpai.orchestrator.repository.ReportRepository;
import pl.corpai.report.service.CrbrSectionAppender;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisOrchestrationService {

    private final AnalysisRequestRepository analysisRequestRepository;
    private final ReportRepository reportRepository;
    private final KrsApiClient krsApiClient;
    private final CrbrApiClient crbrApiClient;
    private final DataSanitizer dataSanitizer;
    private final AzureFunctionClient azureFunctionClient;
    private final CrbrSectionAppender crbrSectionAppender;
    private final RestTemplate restTemplate;

    public AnalysisResponse startAnalysis(String krs) {
        AnalysisRequestEntity entity = AnalysisRequestEntity.builder()
                .krs(krs)
                .status(AnalysisStatus.PENDING)
                .build();
        entity = analysisRequestRepository.save(entity);
        UUID analysisId = entity.getId();

        processAnalysisAsync(analysisId, krs);

        return AnalysisResponse.builder()
                .analysisId(analysisId)
                .status(AnalysisStatus.PENDING)
                .message("Analiza rozpoczęta. Sprawdź status pod /api/v1/analysis/" + analysisId + "/status")
                .build();
    }

    @Async
    public void processAnalysisAsync(UUID analysisId, String krs) {
        log.info("Rozpoczynam asynchroniczne przetwarzanie analizy {} dla KRS {}", analysisId, krs);

        updateStatus(analysisId, AnalysisStatus.PROCESSING, null);

        try {
            // Step 1: Fetch KRS data
            KrsData krsData;
            try {
                krsData = krsApiClient.fetch(krs);
            } catch (KrsNotFoundException e) {
                log.warn("KRS {} nie znaleziony: {}", krs, e.getMessage());
                updateStatus(analysisId, AnalysisStatus.FAILED, "Spółka o podanym numerze KRS nie istnieje");
                return;
            } catch (Exception e) {
                log.error("Błąd pobierania danych KRS {}: {}", krs, e.getMessage());
                updateStatus(analysisId, AnalysisStatus.FAILED, "Błąd pobierania danych KRS: " + e.getMessage());
                return;
            }

            // Update NIP and company name
            AnalysisRequestEntity entity = analysisRequestRepository.findById(analysisId).orElseThrow();
            entity.setNip(krsData.getNip());
            entity.setCompanyName(krsData.getName());
            analysisRequestRepository.save(entity);

            // Step 2: Fetch CRBR data (non-blocking on failure)
            CrbrData crbrData = crbrApiClient.fetch(krsData.getNip());

            // Step 3: Sanitize data
            SanitizedCompanyPayload sanitizedPayload = dataSanitizer.sanitize(krsData);

            // Step 4: Call Azure Function
            AzureReportResponse azureResponse;
            try {
                azureResponse = azureFunctionClient.generateReport(sanitizedPayload);
            } catch (Exception e) {
                log.error("Błąd wywołania Azure Function: {}", e.getMessage());
                updateStatus(analysisId, AnalysisStatus.FAILED, "Błąd generowania raportu");
                return;
            }

            // Step 5: Download PDF from Blob URL
            byte[] pdfBytes;
            try {
                pdfBytes = restTemplate.getForObject(azureResponse.getBlobUrl(), byte[].class);
            } catch (Exception e) {
                log.error("Błąd pobierania PDF z Blob URL: {}", e.getMessage());
                updateStatus(analysisId, AnalysisStatus.FAILED, "Błąd pobierania wygenerowanego raportu");
                return;
            }

            // Step 6: Append CRBR section (PII stays in SIB zone)
            byte[] finalPdf = crbrSectionAppender.append(pdfBytes, crbrData);

            // Step 7: Save report
            ReportEntity report = ReportEntity.builder()
                    .analysisId(analysisId)
                    .pdfContent(finalPdf)
                    .build();
            reportRepository.save(report);

            // Step 8: Update status to COMPLETED
            entity = analysisRequestRepository.findById(analysisId).orElseThrow();
            entity.setStatus(AnalysisStatus.COMPLETED);
            analysisRequestRepository.save(entity);

            log.info("Analiza {} zakończona sukcesem dla spółki: {}", analysisId, krsData.getName());

        } catch (Exception e) {
            log.error("Nieoczekiwany błąd podczas przetwarzania analizy {}: {}", analysisId, e.getMessage(), e);
            updateStatus(analysisId, AnalysisStatus.FAILED, "Nieoczekiwany błąd: " + e.getMessage());
        }
    }

    public Optional<AnalysisRequestEntity> getAnalysis(UUID analysisId) {
        return analysisRequestRepository.findById(analysisId);
    }

    public Optional<ReportEntity> getReport(UUID analysisId) {
        return reportRepository.findByAnalysisId(analysisId);
    }

    private void updateStatus(UUID analysisId, AnalysisStatus status, String errorMessage) {
        analysisRequestRepository.findById(analysisId).ifPresent(entity -> {
            entity.setStatus(status);
            if (errorMessage != null) {
                entity.setErrorMessage(errorMessage);
            }
            analysisRequestRepository.save(entity);
        });
    }
}
