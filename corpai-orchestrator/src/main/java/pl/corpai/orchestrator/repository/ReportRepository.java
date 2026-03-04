package pl.corpai.orchestrator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.corpai.common.entity.ReportEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<ReportEntity, UUID> {
    Optional<ReportEntity> findByAnalysisId(UUID analysisId);
}
