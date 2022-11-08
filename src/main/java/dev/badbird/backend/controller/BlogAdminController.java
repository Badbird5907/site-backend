package dev.badbird.backend.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.badbird.backend.model.Blog;
import dev.badbird.backend.model.ETagIcon;
import dev.badbird.backend.model.Tag;
import dev.badbird.backend.object.Author;
import dev.badbird.backend.object.GithubReference;
import dev.badbird.backend.object.Location;
import dev.badbird.backend.repositories.BlogRepository;
import dev.badbird.backend.repositories.TagsRepository;
import dev.badbird.backend.security.UserDetailsImpl;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Optional;

@RequestMapping("/blog")
@RestController
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_USER')")
public class BlogAdminController {
    @Autowired
    private BlogRepository blogRepository;
    @Autowired
    private TagsRepository tagsRepository;
    @Autowired
    private Gson gson;

    @PostMapping("/create")
    public ResponseEntity<?> createBlog(@RequestBody @Valid BlogAdminController.CreateEditBlogRequest request) {
        Location location;
        if (request.githubURL != null && !request.githubURL.isEmpty()) {
            location = Location.fromGithubRef(GithubReference.fromURL(request.githubURL));
        } else if (request.directURL != null && !request.directURL.isEmpty()) {
            location = Location.fromDirectURL(request.directURL);
        } else if (request.content != null && !request.content.isEmpty()) {
            location = Location.fromContents(request.content);
        } else {
            return ResponseEntity.badRequest().body("{\"success\": false, \"error\": \"No content provided\"}");
        }
        request.title = request.title.trim();
        if (request.title.isEmpty()) {
            return ResponseEntity.badRequest().body("{\"success\": false, \"error\": \"Title cannot be empty\"}");
        }
        if (blogRepository.existsByTitle(request.title)) {
            return ResponseEntity.badRequest().body("{\"success\": false, \"error\": \"Title already exists\"}");
        }
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        System.out.println("Principal: " + principal);
        String userId = ((UserDetailsImpl) principal).getId();
        Author author;
        if (request.customAuthor != null && !request.customAuthor.isEmpty()) {
            author = Author.fromCustom(request.customAuthor, request.customAuthorImg);
        } else {
            author = Author.fromUser(userId);
        }
        Blog blog = new Blog(request.title, request.description, location, author, userId);
        if (request.tags != null && !request.tags.isEmpty()) {
            for (String tag : request.tags) {
                if (tagsRepository.findById(tag).isEmpty()) {
                    return ResponseEntity.badRequest().body("{\"success\": false, \"error\": \"Invalid tag UUID provided\"}");
                }
            }
            blog.setTags(request.tags);
        }
        long timestamp = request.timestamp;
        if (timestamp <= 0) timestamp = System.currentTimeMillis();
        blog.setTimestamp(timestamp);
        if (request.imageURL != null && !request.imageURL.isEmpty()) {
            blog.setImage(request.imageURL);
        }
        blogRepository.save(blog);
        return ResponseEntity.ok("{\"success\": true, \"id\": \"" + blog.getId() + "\", \"url\": \"" + blog.getURLSafeTitle() + "\"}");
    }

    @PostMapping("/edit/{id}")
    public ResponseEntity<?> editBlog(@RequestBody @Valid CreateEditBlogRequest request) {
        if (request.id == null || request.id.isEmpty()) {
            return ResponseEntity.badRequest().body("{\"success\": false, \"error\": \"No id provided\"}");
        }
        request.title = request.title.trim();
        if (request.title.isEmpty()) {
            return ResponseEntity.badRequest().body("{\"success\": false, \"error\": \"Title cannot be empty\"}");
        }
        if (blogRepository.existsByTitle(request.title) && !blogRepository.findByTitle(request.title).get(0).getId().equals(request.id)) {
            return ResponseEntity.badRequest().body("{\"success\": false, \"error\": \"Title already exists\"}");
        }
        Optional<Blog> blogOptional = blogRepository.findById(request.id);
        if (blogOptional.isEmpty())
            return ResponseEntity.badRequest().body("{\"success\": false, \"error\": \"Blog not found\"}");
        Blog blog = blogOptional.get();
        if (request.title != null && !request.title.isEmpty()) blog.setTitle(request.title);
        if (request.description != null && !request.description.isEmpty()) blog.setDescription(request.description);
        if (request.githubURL != null && !request.githubURL.isEmpty()) {
            blog.setLocation(Location.fromGithubRef(GithubReference.fromURL(request.githubURL)));
        } else if (request.directURL != null && !request.directURL.isEmpty()) {
            blog.setLocation(Location.fromDirectURL(request.directURL));
        } else if (request.content != null && !request.content.isEmpty()) {
            blog.setLocation(Location.fromContents(request.content));
        }
        if (request.customAuthor != null && !request.customAuthor.isEmpty()) {
            blog.setAuthor(Author.fromCustom(request.customAuthor, request.customAuthorImg));
        } else {
            blog.setAuthor(Author.fromUser(blog.getCreator()));
        }
        if (request.tags != null) { // Not checking empty because it's possible to remove all tags
            for (String tag : request.tags) {
                if (tagsRepository.findById(tag).isEmpty()) {
                    return ResponseEntity.badRequest().body("{\"success\": false, \"error\": \"Invalid tag UUID provided\"}");
                }
            }
            blog.setTags(request.tags);
        }
        if (request.imageURL != null && !request.imageURL.isEmpty()) {
            blog.setImage(request.imageURL);
        }
        long timestamp = request.timestamp;
        if (timestamp <= 0) timestamp = System.currentTimeMillis();
        blog.setTimestamp(timestamp);
        blogRepository.save(blog);
        return ResponseEntity.ok("{\"success\": true, \"id\": \"" + blog.getId() + "\", \"url\": \"" + blog.getURLSafeTitle() + "\"}");
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteBlog(@PathVariable String id) {
        Optional<Blog> optionalBlog = blogRepository.findById(id);
        if (optionalBlog.isEmpty())
            return ResponseEntity.badRequest().body("{\"success\": false, \"error\": \"Blog not found\"}");
        Blog blog = optionalBlog.get();
        String ownerId = blog.getCreator();
        UserDetailsImpl user = ((UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        String userId = user.getId();
        if (!ownerId.equals(userId) && !user.isAdmin()) {
            return ResponseEntity.badRequest().body("{\"success\": false, \"message\": \"You do not have permission to delete this blog\"}");
        }
        blogRepository.delete(blog);
        return ResponseEntity.ok("{\"success\": true}");
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class DeleteTagRequest {
        @NotBlank
        public String id;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class CreateEditBlogRequest {
        @NotBlank
        private String title;
        private String description;
        private String content;
        private List<String> tags; // tag ids
        private String customAuthor;
        private String customAuthorImg;

        private String contents, directURL, githubURL;
        private String imageURL;
        private long timestamp = -1;

        private String id;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class DeleteBlogRequest {
        @NotBlank
        private String id;
    }
}
