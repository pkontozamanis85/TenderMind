# TenderMind: AI-Driven Procurement and Semantic Recommendation System

TenderMind is an integrated software ecosystem designed to automate the discovery and matching of public tender opportunities. By combining a Java Spring Boot backend for data orchestration with a Python-based Natural Language Processing (NLP) engine, the system identifies relevant contracts through semantic context rather than simple keyword matching.

---

## Core Innovation: Hybrid Semantic Matching

The primary challenge in public procurement is the linguistic variation in tender titles. Standard search engines often fail to connect a contractor's specific interests with relevant tenders if the terminology differs.

TenderMind solves this through a Hybrid Recommendation Engine combining AI and business rules:
1. **Semantic Vectorization (30% weight):** The system converts tender descriptions and user interests into high-dimensional mathematical vectors using the all-MiniLM-L6-v2 transformer model. It calculates the cosine similarity to identify matches based on meaning (e.g., connecting "Green Energy" with "Photovoltaic Installation") even when no common words exist.
2. **Industry Relevance (40% weight):** Implements hierarchical prefix matching on CPV (Common Procurement Vocabulary) codes to ensure recommendations strongly align with the contractor's specific sector.
3. **Geographical Proximity (30% weight):** Evaluates NUTS (Nomenclature of Territorial Units for Statistics) codes to prioritize tenders within or near the user's operational region.

---

## Technical Architecture

The project utilizes a decoupled architecture to separate concerns between data management and machine learning:

- Java Backend (Spring Boot): Handles real-time communication with the Greek eProcurement Open Data API, manages the H2 persistence layer, and orchestrates the execution of the ML environment.
- ML Engine (Python): Executes the NLP logic, utilizing Sentence-Transformers and Pandas to perform large-scale semantic analysis on the ingested dataset.
- System Bridge: A dedicated controller within the Java environment manages the lifecycle of the Python process, piping its output directly to the primary system logs for unified monitoring.

---

## Prerequisites

Before running the system, ensure you have the following installed:
- **Java Development Kit (JDK) 17+**
- **Maven 3.6+** (for building the Spring Boot app)
- **Python 3.9+**
- **Python Libraries:**
### Python Dependencies
The AI Matching Engine requires specific libraries to handle vectorization and data processing. Install them using the command appropriate for your Operating System:

**For Windows:**
```bash
py -m pip install pandas sentence-transformers requests torch
```

**For MacOS/Linux:**
```bash
python -m pip install pandas sentence-transformers requests torch
```
---

## Key Engineering Features

- Strict Financial Filtering: Applies boolean masking algorithms to ensure recommendations strictly adhere to a contractor's minimum and maximum budget capacity.
- Live Data Ingestion: Automated synchronization with the cerpp.eprocurement.gov.gr API.
- Safety Pagination Logic: Implemented MAX_PAGES_LIMIT to ensure low-latency responses and prevent system timeouts during bulk data transfers.
- Idempotent Pipeline: Advanced duplicate prevention logic using unique reference numbers to maintain data integrity in the persistence layer.
- Automated Seeding: A pre-configured data.sql script initializes professional profiles (Civil Engineers, IT Consultants) and their complex interest networks upon startup.
- Process Execution Management: Integrated Java-to-Python execution bridge allowing the ML engine to be triggered via standard REST endpoints.

---
### System Workflow

1. **Data Ingestion:** The Java `SearchService` fetches paginated data from the government API. It uses a path-agnostic mapping strategy to resolve CPV and NUTS codes from inconsistent JSON structures.
2. **Asynchronous Orchestration:** Ingestion and Matching are handled via `CompletableFuture` and `ProcessBuilder`, ensuring the REST API remains responsive (returning 202 Accepted) while heavy tasks run in the background.
3. **Semantic Scoring:** The Python engine calculates a weighted score for each candidate:
   $$Final Score = (0.3 \times Semantic) + (0.4 \times CPV) + (0.3 \times Location)$$
4. **Output:** Personalized JSON reports are generated in the `/recommendations` directory.

---

## Getting Started

### 1. Backend Initialization
Execute the Spring Boot application. The H2 database will automatically provision the database schema and seed the initial user profiles.
Swagger Documentation: http://localhost:8080/swagger-ui.html

### 2. Data Acquisition
Use the api_tests.http file to trigger the ingestion requests. This populates the local persistence layer with live tenders categorized by CPV (Common Procurement Vocabulary) codes or keywords.

### 3. AI Execution
Trigger the Semantic Matching process via the Following REST endpoint:
POST http://localhost:8080/api/matching/run

The system will execute the Python logic and generate personalized recommendation reports in the /recommendations directory.

---
Developed for the modernization of Public Procurement discovery.
