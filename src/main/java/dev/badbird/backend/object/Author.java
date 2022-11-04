package dev.badbird.backend.object;

import dev.badbird.backend.model.User;
import dev.badbird.backend.repositories.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Optional;

@AllArgsConstructor
@Data
public class Author {
    private String authorId; // Author of the blog (UUID id)
    private String customAuthor = null, customAuthorImage = null; // Custom author name and image

    public boolean isCustom() {
        return customAuthor != null && !customAuthor.isEmpty();
    }

    public String getAuthorName(UserRepository userRepository) {
        if (isCustom()) return customAuthor;
        if (authorId == null) return "Unknown";
        Optional<User> user = userRepository.findById(authorId);
        if (user.isPresent()) return user.get().getUsername();
        return "Unknown";
    }
    public String getAuthorImage(UserRepository userRepository) {
        if (customAuthorImage != null && !customAuthorImage.isEmpty()) return customAuthorImage;
        if (authorId == null) {
            return User.DEFAULT_PROFILE;
        }
        Optional<User> user = userRepository.findById(authorId);
        if (user.isPresent()) return user.get().getImageUrl();
        return User.DEFAULT_PROFILE;
    }

    public static Author fromUser(User user) {
        return new Author(user.getId(), null, null);
    }
    public static Author fromUser(String id) {
        return new Author(id, null, null);
    }

    public static Author fromCustom(String customAuthor, String customAuthorImage) {
        return new Author(null, customAuthor, customAuthorImage);
    }
}
