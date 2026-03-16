package TenderMind.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

/**
 * Represents a registered contractor/user in the system.
 * This entity stores profile data and matching preferences.
 */
@Entity
@Table(name = "Users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "interests") // Exclude lazy/circular relations from toString
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    private Integer age;
    private String sex;
    private String profession;

    // Matching criteria: Budget range
    private Double preferredCostMin;
    private Double preferredCostMax;

    /**
     * Stores a simple list of organizations the user is interested in.
     * Stored in a separate collection table.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_preferred_organizations", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "organization")
    private List<String> preferredOrganizations;

    // Geographic and Industry classification filters
    private String nutsCode; // Regional code (e.g., EL30)
    private String cpvCode;  // Industry code (e.g., 45000000)

    /**
     * Version field for Optimistic Locking.
     * Prevents "Lost Update" anomalies during concurrent editing.
     */
    @Version
    private Long version;

    /**
     * Relationship with Interest entity.
     * Uses CascadeType.PERSIST/MERGE so interests are managed via the User object.
     */
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "user_interests",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "interest_id")
    )
    private List<Interest> interests;
}