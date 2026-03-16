package TenderMind.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Represents a public tender notice (Bid) fetched from external sources.
 * Optimized with indexes for high-performance searching.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Table(
        name = "Bid",
        indexes = {
                @Index(name = "idx_bid_reference", columnList = "referenceNumber", unique = true),
                @Index(name = "idx_bid_date", columnList = "date")
        }
)
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The unique government reference ID for this bid.
     * Must be unique to prevent ingestion of duplicates.
     */
    @Column(unique = true, nullable = false)
    private String referenceNumber;

    @Column(length = 500)
    private String title;

    // Explicit JSON property names for integration with external ML scripts
    @JsonProperty("cpvCode")
    private String cpvCode;

    private String organization;
    private LocalDate date;
    private Double cost;

    @JsonProperty("nutsCode")
    private String nutsCode;

    @Column(length = 2000)
    private String description;

    /**
     * Audit field to track when the record was ingested into our system.
     */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Automatically sets the creation timestamp before the record is persisted.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // Standard constructor for manual ingestion mapping
    public Bid(String referenceNumber, String title, String description, String cpvCode, String organization, LocalDate date, Double cost, String nutsCode) {
        this.referenceNumber = referenceNumber;
        this.title = title;
        this.description = description;
        this.cpvCode = cpvCode;
        this.organization = organization;
        this.date = date;
        this.cost = cost;
        this.nutsCode = nutsCode;
    }
}