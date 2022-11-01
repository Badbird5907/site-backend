package dev.badbird.backend.repositories;

import dev.badbird.backend.model.Blog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
import java.util.UUID;

public interface BlogRepository extends MongoRepository<Blog, String> {
    Optional<Blog> findById(UUID id);

}
