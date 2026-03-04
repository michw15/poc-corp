package pl.corpai.orchestrator.exception;

public class KrsNotFoundException extends RuntimeException {
    public KrsNotFoundException(String krs) {
        super("Spółka o podanym numerze KRS nie istnieje: " + krs);
    }
}
