package dev.badbird.backend.object;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MarkdownComponent {
    private Location location;
    private String name;

    private transient String contents;
    public String getContents() {
        if (contents != null) return contents;
        return contents = location.getContents();
    }
}
