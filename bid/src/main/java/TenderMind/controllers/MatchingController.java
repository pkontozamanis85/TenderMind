package TenderMind.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * REST Controller responsible for orchestrating the AI Matching Engine.
 * This class acts as a bridge between the JVM and the Python ML environment,
 * triggering the semantic analysis on demand.
 */
@Slf4j
@RestController
@RequestMapping("/api/matching")
@Tag(name = "AI Matching Engine", description = "Operations to trigger and monitor the Python NLP matching process")
public class MatchingController {

    /**
     * Executes the external Python script responsible for Semantic Matching.
     * It captures the script's output stream and logs it in real-time within the Java console.
     * * @return 200 OK with the execution log if successful, 500 Internal Server Error otherwise.
     */
    @PostMapping("/run")
    @Operation(summary = "Execute AI Semantic Matching", description = "Triggers the Python script to match users with collected bids using NLP.")
    public ResponseEntity<String> runMatching() {
        try {
            log.info("AI ENGINE: Initiating cross-platform execution...");

            /* ProcessBuilder is used to launch the Python script as a separate system process.
               We point to the relative path of the script from the project root.
            */
            ProcessBuilder pb = new ProcessBuilder("python", "Matching/Matching.py");
            pb.redirectErrorStream(true); // Merges Error Stream with Input Stream for easier logging

            Process process = pb.start();

            // Capturing the output of the Python script to display it in Java logs
            StringBuilder pythonOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    pythonOutput.append(line).append("\n");
                    log.info("[Python Engine] {}", line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                log.info("AI ENGINE: Matching sequence completed successfully (Exit Code 0).");
                return ResponseEntity.ok("Matching completed successfully! \n\nLog:\n" + pythonOutput);
            } else {
                log.error("AI ENGINE: Python script terminated with errors. Exit Code: {}", exitCode);
                return ResponseEntity.internalServerError().body("Matching Engine Failure. Check script logs.");
            }

        } catch (Exception e) {
            log.error("CRITICAL FAILURE: Could not establish communication with Python Engine", e);
            return ResponseEntity.internalServerError().body("System Bridge Error: " + e.getMessage());
        }
    }
}