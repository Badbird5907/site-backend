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
        String userId = ((UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();
        Author author;
        if (request.customAuthor != null && !request.customAuthor.isEmpty()) {
            author = Author.fromCustom(request.customAuthor, request.customAuthorImg);
        } else {
            author = Author.fromUser(userId);
        }
        Blog blog = new Blog(request.title, request.description, location, author, userId);
        blog.setTags(request.tags);
        blog.setTimestamp(System.currentTimeMillis());
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
            blog.setAuthor(Author.fromUser(blog.getAuthor().getAuthorId()));
        }
        if (request.tags != null && !request.tags.isEmpty()) {
            blog.setTags(request.tags);
        }
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

    @GetMapping("/tags/get")
    public ResponseEntity<String> getTags() {
        JsonObject jsonObject = new JsonObject();
        List<Tag> tags = tagsRepository.findAll();
        jsonObject.add("tags", gson.toJsonTree(tags));
        jsonObject.addProperty("success", true);
        return ResponseEntity.ok(gson.toJson(jsonObject));
    }

    @PostMapping("/tags/add")
    public ResponseEntity<String> addTag(@RequestBody @Valid CreateTagRequest data) {
        if (tagsRepository.findByName(data.name).isPresent()) {
            return ResponseEntity.ok("{\"success\": false, \"error\": \"Tag already exists\"}");
        }
        ETagIcon icon = ETagIcon.NONE;
        try {
            if (data.icon != null && !data.icon.isEmpty()) {
                icon = ETagIcon.valueOf(data.icon);
            }
        } catch (IllegalArgumentException ignored) {
        }
        Tag tag = new Tag(data.name, data.description, icon);
        tagsRepository.save(tag);
        return ResponseEntity.ok("{\"success\": true, \"id\": \"" + tag.getId() + "\", \"name\": \"" + tag.getName() + "\", \"description\": \"" + tag.getDescription() + "\", \"icon\": \"" + tag.getIcon().name() + "\"}");
    }

    @PostMapping("/tags/edit")
    public ResponseEntity<String> editTag(@RequestBody @Valid EditTagRequest data) {
        Optional<Tag> optionalTag = tagsRepository.findById(data.id);
        if (optionalTag.isEmpty()) {
            return ResponseEntity.ok("{\"success\": false, \"error\": \"Tag does not exist\"}");
        }
        Tag tag = optionalTag.get();
        tag.setName(data.name);
        tag.setDescription(data.description);
        ETagIcon icon = ETagIcon.NONE;
        try {
            if (data.icon != null && !data.icon.isEmpty()) {
                icon = ETagIcon.valueOf(data.icon);
            }
        } catch (IllegalArgumentException ignored) {
        }
        tag.setIcon(icon);
        tagsRepository.save(tag);
        return ResponseEntity.ok("{\"success\": true, \"id\": \"" + tag.getId() + "\", \"name\": \"" + tag.getName() + "\", \"description\": \"" + tag.getDescription() + "\", \"icon\": \"" + tag.getIcon().name() + "\"}");
    }

    @PostMapping("/tags/delete")
    public ResponseEntity<String> deleteTag(@RequestBody @Valid DeleteTagRequest data) {
        Optional<Tag> optionalTag = tagsRepository.findById(data.id);
        if (optionalTag.isEmpty()) {
            return ResponseEntity.ok("{\"success\": false, \"error\": \"Tag does not exist\"}");
        }
        Tag tag = optionalTag.get();
        tagsRepository.delete(tag);
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
    public static class EditTagRequest {
        @NotBlank
        public String id;
        @NotBlank
        public String name;
        @NotBlank
        public String description;
        public String icon;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class CreateTagRequest {
        @NotBlank
        private String name;
        @NotBlank
        private String description;
        @NotBlank
        private String icon;
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
