package dev.badbird.backend.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.badbird.backend.model.Blog;
import dev.badbird.backend.object.Location;
import dev.badbird.backend.repositories.BlogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RequestMapping("/api/blog")
@RestController
@CrossOrigin(origins = "*")
public class BlogController {
    private static final String BLOG_NOT_FOUND = "{\"success\": false, \"error\": \"Blog not found\", \"code\": 404}";
    @Autowired
    private BlogRepository blogRepository;
    @Autowired
    private Gson gson;

    @GetMapping("/meta/get/{id}")
    public ResponseEntity<?> getBlogInfo(@PathVariable("id") String idStr) {
        Optional<Blog> optionalBlog = blogRepository.findById(idStr);
        if (!optionalBlog.isPresent()) {
            return ResponseEntity.status(404)
                    .body(BLOG_NOT_FOUND);
        }
        return ResponseEntity.ok(gson.toJson(getBlogMeta(optionalBlog)));
    }

    @GetMapping("/content/get/{id}") // More expensive, so it's a separate endpoint
    public ResponseEntity<?> getBlogContent(@PathVariable("id") String idStr) {
        System.out.println("Getting blog content");
        Optional<Blog> optionalBlog = blogRepository.findById(idStr);
        if (!optionalBlog.isPresent()) {
            return ResponseEntity.status(404)
                    .body(BLOG_NOT_FOUND);
        }
        try {
            JsonObject data = getBlogMeta(optionalBlog);
            data.addProperty("content", optionalBlog.get().getContent());
            return ResponseEntity.ok(gson.toJson(data));
        } catch (Exception e) {
            String githubURL;
            Location location = optionalBlog.get().getLocation();
            if (location.getGithubReference() != null) {
                githubURL = location.getGithubReference().getEffectiveURL(false);
                return ResponseEntity.status(500)
                        .body("{\"success\": false, \"error\": \"Error getting blog content\", \"code\": 500, \"githubURL\": \"" + githubURL + "\"}");
            }
            return ResponseEntity.status(500)
                    .body("{\"success\": false, \"error\": \"Internal server error\", \"code\": 500}");
        }
    }

    public JsonObject getBlogMeta(Optional<Blog> optionalBlog) {
        Blog blog = optionalBlog.get();
        JsonObject jsonObject = gson.toJsonTree(blog)
                .getAsJsonObject();
        jsonObject.addProperty("success", true);
        jsonObject.remove("cached");
        return jsonObject;
    }

}
