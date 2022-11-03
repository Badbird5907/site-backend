package dev.badbird.backend.repositories;

import dev.badbird.backend.model.Blog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface BlogRepository extends MongoRepository<Blog, String> {
    Optional<Blog> findById(String id);

    List<Blog> findByTitle(String urlEncodedTitle);

    Page<Blog> findAllByOrderByTimestampDesc(Pageable pageable);

    Boolean existsByTitle(String title);
}
