package dev.badbird.backend.repositories;

import dev.badbird.backend.model.Tag;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TagsRepository extends MongoRepository<Tag, String> {
    Optional<Tag> findById(String id);

    Optional<Tag> findByName(String name);
}
