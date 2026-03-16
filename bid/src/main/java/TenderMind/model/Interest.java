package TenderMind.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

/**
 * Represents a keyword or tag (e.g., "Construction", "Software")
 * that users can associate with their profiles.
 */
@Entity
@Table(name = "Interests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "users")
public class Interest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    /**
     * JsonCreator allows Jackson to deserialize a simple string into an Interest object.
     */
    @JsonCreator
    public Interest(@JsonProperty("name") String name) {
        this.name = name;
    }

    /**
     * Bi-directional relationship with User.
     * JsonIgnore is crucial to prevent Infinite Recursion during API serialization.
     */
    @JsonIgnore
    @ManyToMany(mappedBy = "interests")
    private List<User> users;
}