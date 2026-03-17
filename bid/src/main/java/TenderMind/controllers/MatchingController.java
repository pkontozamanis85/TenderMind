package TenderMind.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller responsible for orchestrating the AI Matching Engine.
 * This class acts as a bridge between the JVM and the Python ML environment,
 * triggering the semantic analysis asynchronously on demand.
 */
@Slf4j
@RestController
@RequestMapping("/api/matching")
@Tag(name = "AI Matching Engine", description = "Operations to trigger and monitor the Python NLP matching process")
public class MatchingController {

    /**
     * Executes the external Python script asynchronously.
     * Prevents HTTP ReadTimeoutExceptions by immediately returning a 202 ACCEPTED response,
     * while the Python script runs and logs to the Java console in the background.
     */
    @PostMapping("/run")
    @Operation(summary = "Execute AI Semantic Matching (Async)", description = "Triggers the Python script in the background to avoid timeouts on large datasets.")
    public ResponseEntity<String> runMatching() {

        log.info("AI ENGINE: Request received. Initiating background cross-platform execution...");

        // Start the heavy Python process in a separate background thread
        CompletableFuture.runAsync(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder("python", "Matching/Matching.py");
                pb.redirectErrorStream(true);

                Process process = pb.start();

                // Read output in real-time and log it to the Spring Boot console
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("[Python Engine] {}", line);
                    }
                }

                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    log.info("AI ENGINE: Background matching sequence completed successfully (Exit Code 0).");
                } else {
                    log.error("AI ENGINE: Python script terminated with errors. Exit Code: {}", exitCode);
                }

            } catch (Exception e) {
                log.error("CRITICAL FAILURE: Could not establish communication with Python Engine", e);
            }
        });

        // Immediately return response to the client (Swagger/Postman) so it doesn't timeout
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body("Matching Engine successfully started in the background! Please check the Java console for real-time progress and logs.");
    }
}