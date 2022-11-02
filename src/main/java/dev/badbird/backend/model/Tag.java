package dev.badbird.backend.model;

import dev.badbird.backend.repositories.TagsRepository;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Data
@Document(collection = "tags")
public class Tag {
    @Id
    private String id;
    private String name;
    private String description;
    private ETagIcon icon;

    public Tag(String name, String description, ETagIcon icon) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.icon = icon;
    }
}
