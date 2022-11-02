package dev.badbird.backend.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set;
import java.util.UUID;

@Document(collection = "users")
@Getter
@Setter
public class User {
    public static final String DEFAULT_PROFILE =  "https://cdn.badbird.dev/assets/user.jpg";
    @Id
    private String id;

    private String username;

    private String password;
    private String imageUrl = DEFAULT_PROFILE;

    @DBRef
    private Set<Role> roles;

    public User() {
    }

    public User(String username, String password) {
        this.id = UUID.randomUUID().toString();
        this.username = username;
        this.password = password;
    }
}
