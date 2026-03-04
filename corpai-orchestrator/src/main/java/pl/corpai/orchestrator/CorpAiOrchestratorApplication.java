package pl.corpai.orchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EntityScan(basePackages = {"pl.corpai.common.entity"})
@EnableJpaRepositories(basePackages = {"pl.corpai.orchestrator.repository"})
@ComponentScan(basePackages = {"pl.corpai.orchestrator", "pl.corpai.report"})
public class CorpAiOrchestratorApplication {
    public static void main(String[] args) {
        SpringApplication.run(CorpAiOrchestratorApplication.class, args);
    }
}
