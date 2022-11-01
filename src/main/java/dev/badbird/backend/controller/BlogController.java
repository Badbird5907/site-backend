package dev.badbird.backend.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.badbird.backend.model.Blog;
import dev.badbird.backend.repositories.BlogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

@RequestMapping("/api/blog")
@RestController
public class BlogController {
    private static final String BLOG_NOT_FOUND = "{\"success\": false, \"error\": \"Blog not found\", \"code\": 404}";
    @Autowired
    private BlogRepository blogRepository;
    @Autowired
    private Gson gson;

    @GetMapping("/meta/get/{id}")
    public ResponseEntity<?> getBlogInfo(@RequestParam("id") String idStr) {
        UUID id = UUID.fromString(idStr);
        Optional<Blog> optionalBlog = blogRepository.findById(id);
        if (!optionalBlog.isPresent()) {
            return ResponseEntity.status(404)
                    .body(BLOG_NOT_FOUND);
        }
        return ResponseEntity.ok(gson.toJson(getBlogMeta(optionalBlog)));
    }

    @GetMapping("/content/get/{id}")
    public ResponseEntity<?> getBlogContent(@RequestParam("id") String idStr) {
        UUID id = UUID.fromString(idStr);
        Optional<Blog> optionalBlog = blogRepository.findById(id);
        if (!optionalBlog.isPresent()) {
            return ResponseEntity.status(404)
                    .body(BLOG_NOT_FOUND);
        }
        JsonObject data = getBlogMeta(optionalBlog);
        data.addProperty("content", optionalBlog.get().getContent());
        throw new RuntimeException("Not implemented");
        //return ResponseEntity.ok("");
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
