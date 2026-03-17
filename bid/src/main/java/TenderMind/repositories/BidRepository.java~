package TenderMind.repositories;

import TenderMind.model.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data Access Object for the Bid entity.
 * Handles database operations for government tender notices.
 */
@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

    /**
     * Locates a Bid by its unique reference number.
     * Utilizes the 'idx_bid_reference' database index for $O(1)$ lookup performance.
     *
     * @param referenceNumber The exact registry number of the bid.
     * @return An Optional containing the Bid if found.
     */
    Optional<Bid> findByReferenceNumber(String referenceNumber);
}