package dev.badbird.backend.model;

import dev.badbird.backend.object.Location;
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
    private String authorId; // Author of the blog (UUID id)
    private long timestamp; // Date of the blog
    private String image; // Image URL of the blog
    private List<String> tags; // Tags of the blog (UUID ids)

    private Location location; // Location of the blog

    public Blog(String title, String description, Location location, User author) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.description = description;
        this.timestamp = System.currentTimeMillis();
        this.location = location;
        this.authorId = author.getId();
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
}
