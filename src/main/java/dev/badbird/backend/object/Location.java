package dev.badbird.backend.object;

import dev.badbird.backend.util.WebUtil;
import lombok.Data;

@Data
public class Location {
    private String contents;
    private String directURL;
    private GithubReference githubReference;

    private Location() {

    }

    public static Location fromContents(String contents) {
        Location location = new Location();
        location.setContents(contents);
        return location;
    }

    public static Location fromDirectURL(String contents) {
        Location location = new Location();
        location.setDirectURL(contents);
        return location;
    }

    public static Location fromGithubRef(GithubReference githubReference) {
        Location location = new Location();
        location.setGithubReference(githubReference);
        return location;
    }

    public String getContents() {
        if (contents != null) return contents;
        if (directURL != null) {
            return WebUtil.getURLContents(directURL);
        }
        if (githubReference != null) {
            return githubReference.getFileContents();
        }
        return null;
    }
}
