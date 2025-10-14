package eu.qualco.bid.controllers;

import eu.qualco.bid.model.User;
import eu.qualco.bid.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        log.info("Creating user: {}", user);
        User savedUser = userService.createUser(user);
        log.info("User created with ID: {}", savedUser.getId());
        return ResponseEntity.ok(savedUser);
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        log.info("Fetching all users");
        List<User> users = userService.getAllUsers();
        log.info("Total users found: {}", users.size());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        log.info("Fetching user with ID: {}", id);
        Optional<User> user = userService.getUserById(id);
        if (user.isPresent()) {
            log.info("User found: {}", user.get());
            return ResponseEntity.ok(user.get());
        } else {
            log.warn("User with ID {} not found", id);
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User updatedUser) {
        log.info("Updating user with ID: {} with data: {}", id, updatedUser);
        Optional<User> user = userService.updateUser(id, updatedUser);
        if (user.isPresent()) {
            log.info("User updated: {}", user.get());
            return ResponseEntity.ok(user.get());
        } else {
            log.warn("User with ID {} not found for update", id);
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("Attempting to delete user with ID: {}", id);
        boolean deleted = userService.deleteUser(id);
        if (deleted) {
            log.info("User with ID {} deleted successfully", id);
            return ResponseEntity.ok().build();
        } else {
            log.warn("User with ID {} not found for deletion", id);
            return ResponseEntity.notFound().build();
        }
    }
}