package dev.badbird.backend.repositories;

import dev.badbird.backend.model.ERole;
import dev.badbird.backend.model.Role;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface RoleRepository extends MongoRepository<Role, String> {
    Optional<Role> findByName(ERole name);
}
