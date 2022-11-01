package dev.badbird.backend.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Blog {
    @Id
    private UUID id; // UUID id, can't go wrong with that

    // Stuff shown on the main page
    private String title; // Display title on list
    private String description; // Description of the blog
    private String author; // Author of the blog
    private String date; // Date of the blog
    private String image; // Image of the blog
    private String authorImage; // Image of the author

    private boolean cached = true;

    public String getContent() {
        throw new RuntimeException("Not implemented");
    }
}
