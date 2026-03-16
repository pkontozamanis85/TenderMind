package TenderMind.repositories;

import TenderMind.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data Access Object for the User entity.
 * Extends JpaRepository to inherit standard CRUD and pagination functionality.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}