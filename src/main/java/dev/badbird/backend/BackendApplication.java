package dev.badbird.backend;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.badbird.backend.model.Blog;
import dev.badbird.backend.object.GithubReference;
import dev.badbird.backend.object.Location;
import dev.badbird.backend.repositories.BlogRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.UUID;

@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        File config = new File("config.json");
        if (config.exists()) {
            try {
                String json = new String(Files.readAllBytes(config.toPath()));
                JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
                jsonObject.entrySet().forEach((entry)-> {
                    if (entry.getValue().isJsonPrimitive()) {
                        System.setProperty(entry.getKey(), entry.getValue().getAsString());
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        SpringApplication.run(BackendApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(BlogRepository blogRepository) {
        return args -> {
            Location location = new Location();
            location.setGithubReference(GithubReference.fromURL("https://github.com/Badbird5907/blog/blob/master/content/test/Test.md"));
            Blog blog = new Blog(new UUID(0,0).toString(), "Test Blog", "This is a test blog", "Badbird5907", System.currentTimeMillis(), null, "https://cdn.badbird.dev/assets/profile.gif", Arrays.asList("Hello", "123", "ABC"), false, location);
            blogRepository.save(blog);
            System.out.println("Saved blog");
        };
    }

}
