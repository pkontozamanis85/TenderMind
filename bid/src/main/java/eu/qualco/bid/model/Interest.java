package eu.qualco.bid.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "Interests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Interest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @JsonCreator
    public Interest(@JsonProperty("name") String name) {
        this.name = name;
    }


    @JsonIgnore
    @ManyToMany(mappedBy = "interests")
    private List<User> users;
}
