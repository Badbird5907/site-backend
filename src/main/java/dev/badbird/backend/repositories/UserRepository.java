package dev.badbird.backend.repositories;

import dev.badbird.backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);
    Optional<User> findById(String id);

    Boolean existsByUsername(String username);
}
