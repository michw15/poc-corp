# CorpAI V1 POC

AI-powered corporate company analysis system — Proof of Concept.

## Architektura

System składa się z dwóch warstw:

### SIB Zone (Spring Boot)
- **corpai-gateway** (port 8079) — Spring Cloud Gateway, routing żądań
- **corpai-orchestrator** (port 8080) — główna logika biznesowa
  - `KrsApiClient` — pobiera dane z KRS API
  - `CrbrApiClient` — pobiera beneficjentów rzeczywistych z CRBR API
  - `DataSanitizer` — usuwa PII przed wysłaniem do Azure
  - `AzureFunctionClient` — HTTP POST do Azure Function
- **corpai-report-generator** — biblioteka do dodawania sekcji CRBR do PDF
- **corpai-common** — współdzielone DTO i encje JPA
- **PostgreSQL** — przechowywanie analiz i raportów

### Azure
- **corpai-azure-function** — Azure Function Java 21
  - `GenerateReportFunction` — HTTP trigger
  - `WebScraper` — scraping (Bing/Google news, przetargi TED, strona firmowa)
  - `AzureOpenAiClient` — GPT-4o generuje narrację raportu
  - `PdfReportGenerator` — iText7 generuje PDF
  - `BlobStorageClient` — przechowywanie PDF w Azure Blob Storage

### Przepływ danych

```
Doradca → POST /api/v1/analysis { krs: "0000543759" }
  → Gateway (8079) → Orchestrator (8080)
  → [SIB] KRS API → dane rejestrowe spółki
  → [SIB] CRBR API → beneficjenci rzeczywiści (PII w SIB zone)
  → [SIB] DataSanitizer → inicjały zamiast nazwisk, brak PESEL/kont
  → [SIB→Azure] Azure Function → scraping + GPT-4o + PDF bez CRBR
  → [SIB] Pobierz PDF z Blob URL
  → [SIB] CrbrSectionAppender → dodaj sekcję CRBR do PDF (PII wraca do SIB)
  → PostgreSQL → zapisz finalny PDF
  → GET /api/v1/analysis/{id}/report → doradca pobiera PDF
```

### Bezpieczeństwo (PII / SIB Zone)

**Kluczowa zasada**: Dane osobowe beneficjentów rzeczywistych (CRBR) — imiona, nazwiska, PESEL — **NIGDY nie opuszczają SIB Zone**.
- `DataSanitizer` zamienia imiona/nazwiska członków zarządu na inicjały (`Jan Kowalski` → `J.K.`)
- `SanitizedCompanyPayload` nie zawiera żadnych danych z `CrbrData`
- Azure Function pracuje wyłącznie na zanonimizowanych danych
- Sekcja CRBR jest dodawana do PDF dopiero po powrocie z Azure, w SIB Zone

## Wymagania

- Java 21
- Maven 3.8+
- Docker & Docker Compose
- Azure CLI (do deployu Azure Function)

## Uruchomienie lokalne

### 1. Baza danych (PostgreSQL)

```bash
docker-compose up -d postgres
```

### 2. Konfiguracja środowiska

Skopiuj `.env.example` do `.env` i wypełnij wartości:

```bash
cp .env.example .env
```

### 3. Moduły SIB Zone

Zbuduj i uruchom moduły:

```bash
# Build całego projektu
mvn clean install -pl corpai-common,corpai-report-generator -am

# Uruchom Gateway
cd corpai-gateway && mvn spring-boot:run &

# Uruchom Orchestrator
cd corpai-orchestrator && mvn spring-boot:run \
  -Dspring-boot.run.jvmArguments="-DAZURE_FUNCTION_URL=${AZURE_FUNCTION_URL} -DAZURE_FUNCTION_KEY=${AZURE_FUNCTION_KEY}"
```

### 4. Azure Function (lokalnie)

```bash
cd corpai-azure-function
cp local.settings.json.example local.settings.json
# Wypełnij local.settings.json
mvn clean package
mvn azure-functions:run
```

## Deploy Azure Function

```bash
cd corpai-azure-function
mvn clean package
mvn azure-functions:deploy
```

## Testowanie

Testowy KRS: `0000543759` (przykładowa spółka)

### Rozpocznij analizę

```bash
curl -X POST http://localhost:8079/api/v1/analysis \
  -H "Content-Type: application/json" \
  -d '{"krs": "0000543759"}'
```

Odpowiedź:
```json
{
  "analysisId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING",
  "message": "Analiza rozpoczęta. Sprawdź status pod /api/v1/analysis/.../status"
}
```

### Sprawdź status

```bash
curl http://localhost:8079/api/v1/analysis/{analysisId}/status
```

### Pobierz raport PDF

```bash
curl -o raport.pdf http://localhost:8079/api/v1/analysis/{analysisId}/report
```

## Struktura projektu

```
poc-corp/
├── pom.xml                          ← parent pom (multi-module)
├── docker-compose.yml
├── .env.example
├── README.md
├── corpai-common/                   ← shared DTOs + JPA entities
├── corpai-gateway/                  ← Spring Cloud Gateway (port 8079)
├── corpai-orchestrator/             ← główna logika SIB (port 8080)
├── corpai-report-generator/         ← PDF post-processing (CRBR)
└── corpai-azure-function/           ← Azure Function (standalone Maven module)
```
