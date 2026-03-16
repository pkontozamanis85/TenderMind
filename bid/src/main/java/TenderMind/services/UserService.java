package TenderMind.services;

import TenderMind.model.Interest;
import TenderMind.model.User;
import TenderMind.repositories.InterestRepository;
import TenderMind.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service layer for User operations.
 * Handles business logic, including interest resolution and transactional updates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final InterestRepository interestRepository;

    /**
     * Creates a user and ensures all interests are persisted/resolved.
     */
    @Transactional
    public User createUser(User user) {
        log.info("Resolving interests for new user: {}", user.getFullName());
        List<Interest> resolvedInterests = resolveInterests(user.getInterests());
        user.setInterests(resolvedInterests);
        return userRepository.save(user);
    }

    /**
     * Retrieves all users from the database.
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Finds a specific user by their ID.
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Updates user details and synchronizes interests.
     */
    @Transactional
    public Optional<User> updateUser(Long id, User updatedUser) {
        return userRepository.findById(id).map(existingUser -> {
            existingUser.setFullName(updatedUser.getFullName());
            existingUser.setAge(updatedUser.getAge());
            existingUser.setSex(updatedUser.getSex());
            existingUser.setProfession(updatedUser.getProfession());
            existingUser.setPreferredCostMin(updatedUser.getPreferredCostMin());
            existingUser.setPreferredCostMax(updatedUser.getPreferredCostMax());
            existingUser.setPreferredOrganizations(updatedUser.getPreferredOrganizations());
            existingUser.setNutsCode(updatedUser.getNutsCode());
            existingUser.setCpvCode(updatedUser.getCpvCode());

            // Re-resolve interests to prevent duplicates on update
            existingUser.setInterests(resolveInterests(updatedUser.getInterests()));

            return userRepository.save(existingUser);
        });
    }

    /**
     * Removes a user by ID if they exist.
     */
    @Transactional
    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Helper method to map interest names to existing or new Interest entities.
     */
    private List<Interest> resolveInterests(List<Interest> incomingInterests) {
        if (incomingInterests == null || incomingInterests.isEmpty()) {
            return List.of();
        }
        return incomingInterests.stream()
                .map(interest -> interestRepository.findByName(interest.getName())
                        .orElseGet(() -> interestRepository.save(new Interest(interest.getName()))))
                .collect(Collectors.toList());
    }
}