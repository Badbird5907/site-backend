package dev.badbird.backend.model;

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
import java.util.Optional;
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

    private String customAuthor = null, customAuthorImage = null; // Custom author name and image

    private Location location; // Location of the blog

    private Blog(String title, String description, Location location) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.description = description;
        this.location = location;
    }
    public Blog(String title, String description, Location location, User author) {
        this(title, description, location);
        this.authorId = author.getId();
    }
    public Blog(String title, String description, Location location, String customAuthor, String customAuthorImage) {
        this(title, description, location);
        this.customAuthor = customAuthor;
        this.customAuthorImage = customAuthorImage;
        this.authorId = null;
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
        return customAuthor != null && !customAuthor.isEmpty();
    }

    public String getAuthorName(UserRepository userRepository) {
        if (hasCustomAuthor()) return customAuthor;
        Optional<User> user = userRepository.findById(authorId);
        if (user.isPresent()) return user.get().getUsername();
        return "Unknown";
    }
    public String getAuthorImage(UserRepository userRepository) {
        if (hasCustomAuthor() && customAuthorImage != null && !customAuthorImage.isEmpty()) return customAuthorImage;
        Optional<User> user = userRepository.findById(authorId);
        if (user.isPresent()) return user.get().getImageUrl();
        return User.DEFAULT_PROFILE;
    }

}
