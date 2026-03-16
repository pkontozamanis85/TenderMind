package TenderMind.repositories;

import TenderMind.model.Interest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data Access Object for user Domain Interests.
 */
@Repository
public interface InterestRepository extends JpaRepository<Interest, Long> {

    /**
     * Resolves an Interest entity by its unique name tag.
     * Used heavily during User creation/updates to prevent duplicate tags in the system.
     */
    Optional<Interest> findByName(String name);
}