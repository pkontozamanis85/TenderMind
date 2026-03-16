# TenderMind: AI-Driven Procurement and Semantic Recommendation System

TenderMind is an integrated software ecosystem designed to automate the discovery and matching of public tender opportunities. By combining a Java Spring Boot backend for data orchestration with a Python-based Natural Language Processing (NLP) engine, the system identifies relevant contracts through semantic context rather than simple keyword matching.

---

## Core Innovation: Semantic Vectorization

The primary challenge in public procurement is the linguistic variation in tender titles. Standard search engines often fail to connect a contractor's specific interests with relevant tenders if the terminology differs.

TenderMind solves this through Semantic Vectorization:
1. The system converts tender descriptions and user interests into high-dimensional mathematical vectors using the all-MiniLM-L6-v2 transformer model.
2. It calculates the cosine similarity between these vectors to determine relevance.
3. This allows the system to identify matches based on meaning (e.g., connecting "Green Energy" with "Photovoltaic Installation") even when no common words exist between the two strings.

---

## Technical Architecture

The project utilizes a decoupled architecture to separate concerns between data management and machine learning:

- Java Backend (Spring Boot): Handles real-time communication with the Greek eProcurement Open Data API, manages the H2 persistence layer, and orchestrates the execution of the ML environment.
- ML Engine (Python): Executes the NLP logic, utilizing Sentence-Transformers and Pandas to perform large-scale semantic analysis on the ingested dataset.
- System Bridge: A dedicated controller within the Java environment manages the lifecycle of the Python process, piping its output directly to the primary system logs for unified monitoring.

---

## Key Engineering Features

- Live Data Ingestion: Automated synchronization with the cerpp.eprocurement.gov.gr API.
- Safety Pagination Logic: Implemented MAX_PAGES_LIMIT to ensure low-latency responses and prevent system timeouts during bulk data transfers.
- Idempotent Pipeline: Advanced duplicate prevention logic using unique reference numbers to maintain data integrity in the persistence layer.
- Automated Seeding: A pre-configured data.sql script initializes professional profiles (Civil Engineers, IT Consultants) and their complex interest networks upon startup.
- Process Execution Management: Integrated Java-to-Python execution bridge allowing the ML engine to be triggered via standard REST endpoints.

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

## Roadmap
- Geographic clustering using NUTS (Nomenclature of Territorial Units for Statistics) codes.
- Financial threshold filtering for targeted contract matching.
- Automated notification service for high-confidence match scores.

---
Developed for the modernization of Public Procurement discovery.
