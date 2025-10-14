package eu.qualco.bid.services;

import eu.qualco.bid.model.Interest;
import eu.qualco.bid.model.User;
import eu.qualco.bid.repositories.InterestRepository;
import eu.qualco.bid.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final InterestRepository interestRepository;

    @Transactional
    public User createUser(User user) {
        List<Interest> resolvedInterests = resolveInterests(user.getInterests());
        user.setInterests(resolvedInterests);
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

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

            List<Interest> resolvedInterests = resolveInterests(updatedUser.getInterests());
            existingUser.setInterests(resolvedInterests);

            return userRepository.save(existingUser);
        });
    }

    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        } else {
            return false;
        }
    }


    private List<Interest> resolveInterests(List<Interest> incomingInterests) {
        return incomingInterests.stream()
                .map(interest -> interestRepository.findByName(interest.getName())
                        .orElseGet(() -> interestRepository.save(new Interest(interest.getName()))))
                .collect(Collectors.toList());
    }
}