package pl.corpai.orchestrator.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.corpai.common.dto.AnalysisRequest;
import pl.corpai.common.dto.AnalysisResponse;
import pl.corpai.common.entity.AnalysisRequestEntity;
import pl.corpai.common.entity.ReportEntity;
import pl.corpai.common.enums.AnalysisStatus;
import pl.corpai.orchestrator.service.AnalysisOrchestrationService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisOrchestrationService orchestrationService;

    @PostMapping
    public ResponseEntity<AnalysisResponse> startAnalysis(@RequestBody AnalysisRequest request) {
        log.info("Przyjęto żądanie analizy dla KRS: {}", request.getKrs());
        AnalysisResponse response = orchestrationService.startAnalysis(request.getKrs());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable UUID id) {
        return orchestrationService.getAnalysis(id)
                .map(entity -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("analysisId", entity.getId());
                    result.put("status", entity.getStatus());
                    result.put("companyName", entity.getCompanyName());
                    result.put("krs", entity.getKrs());
                    result.put("createdAt", entity.getCreatedAt());
                    if (entity.getErrorMessage() != null) {
                        result.put("errorMessage", entity.getErrorMessage());
                    }
                    return ResponseEntity.ok(result);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/report")
    public ResponseEntity<byte[]> getReport(@PathVariable UUID id) {
        return orchestrationService.getAnalysis(id)
                .flatMap(entity -> {
                    if (entity.getStatus() != AnalysisStatus.COMPLETED) {
                        return java.util.Optional.empty();
                    }
                    return orchestrationService.getReport(id)
                            .map(report -> {
                                HttpHeaders headers = new HttpHeaders();
                                headers.setContentType(MediaType.APPLICATION_PDF);
                                headers.setContentDispositionFormData("attachment",
                                        "raport-" + entity.getKrs() + ".pdf");
                                return ResponseEntity.ok()
                                        .headers(headers)
                                        .body(report.getPdfContent());
                            });
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
