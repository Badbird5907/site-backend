package dev.badbird.backend.model;

import dev.badbird.backend.object.Location;
import dev.badbird.backend.util.markdown.TempMarkWrapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "blogs")
public class Blog {
    @Id
    private String id;

    // Stuff shown on the main page
    private String title; // Display title on list
    private String description; // Description of the blog
    private String author; // Author of the blog
    private long timestamp; // Date of the blog
    private String image; // Image of the blog
    private String authorImage; // Image of the author
    private List<String> tags; // Tags of the blog

    private boolean cached = true;

    private Location location; // Location of the blog

    public String getContent() {
        return new TempMarkWrapper(this).getContents();
    }
    public String getWebContents() {
        return location.getContents();
    }
}
