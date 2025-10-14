
package eu.qualco.bid.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.Instant;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "Bid")
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary key for the Bid entity")
    private Long id;

    @Column(unique = true, nullable = false)
    @Comment("Unique reference number for the bid")
    private String referenceNumber;

    @Column(length = 500)
    @Comment("Title of the bid, max 500 characters")
    private String title;

    @Comment("CPV code associated with the bid")
    private String cpvCode;

    @Comment("Organization issuing the bid")
    private String organization;

    @Comment("Date of the bid")
    private LocalDate date;

    @Comment("Estimated cost of the bid")
    private Double cost;

    @Comment("NUTS code representing the geographic region of the contracting authority")
    private String nutsCode;

    @Column(length = 2000)
    @Comment("Detailed description of the bid")
    private String description;


    @Column(nullable = false, updatable = false)
    @Comment("Timestamp when the bid was created")
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

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
