package dev.badbird.backend.model;

import dev.badbird.backend.object.Author;
import dev.badbird.backend.object.Location;
import dev.badbird.backend.repositories.UserRepository;
import dev.badbird.backend.util.markdown.TempMarkWrapper;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Document(collection = "blogs")
public class Blog {
    @Id
    private String id;

    // Stuff shown on the main page
    private String title; // Display title on list
    private String description; // Description of the blog
    private long timestamp; // Date of the blog
    private String imageURL; // Image URL of the blog
    private List<String> tags; // Tags of the blog (UUID ids)
    private String creator; // Creator of the blog (UUID id)
    private Author author;

    private Location location; // Location of the blog

    @Deprecated
    private Blog(String title, String description, Location location) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.description = description;
        this.location = location;
    }

    public Blog(String title, String description, Location location, Author author, String creator) {
        this(title, description, location);
        this.author = author;
        this.creator = creator;
    }


    public Blog() {
    }

    public String getContent() {
        return new TempMarkWrapper(this).getContents();
    }

    public String getWebContents() {
        return location.getContents();
    }

    @SneakyThrows
    public String getURLSafeTitle() {
        return URLEncoder.encode(title, StandardCharsets.UTF_8);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean hasCustomAuthor() {
        return author.isCustom();
    }

    public String getAuthorName(UserRepository userRepository) {
        return author.getAuthorName(userRepository);
    }

    public String getAuthorImage(UserRepository userRepository) {
        return author.getAuthorImage(userRepository);
    }

}
