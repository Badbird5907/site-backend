package dev.badbird.backend.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.badbird.backend.model.ETagIcon;
import dev.badbird.backend.model.Tag;
import dev.badbird.backend.repositories.BlogRepository;
import dev.badbird.backend.repositories.TagsRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Optional;

@RequestMapping("/tags")
@RestController
@CrossOrigin(origins = "*")
public class TagsController {
    @Autowired
    private TagsRepository tagsRepository;
    @Autowired
    private BlogRepository blogRepository;
    @Autowired
    private Gson gson;

    @GetMapping("/get")
    public ResponseEntity<String> getTags() {
        JsonObject jsonObject = new JsonObject();
        List<Tag> tags = tagsRepository.findAll();
        jsonObject.add("tags", gson.toJsonTree(tags));
        jsonObject.addProperty("success", true);
        return ResponseEntity.ok(gson.toJson(jsonObject));
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_USER')")
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

    @PostMapping("/edit")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_USER')")
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

    @PostMapping("/delete")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_USER')")
    public ResponseEntity<String> deleteTag(@RequestBody @Valid BlogAdminController.DeleteTagRequest data) {
        Optional<Tag> optionalTag = tagsRepository.findById(data.id);
        if (optionalTag.isEmpty()) {
            return ResponseEntity.ok("{\"success\": false, \"error\": \"Tag does not exist\"}");
        }
        Tag tag = optionalTag.get();
        tagsRepository.delete(tag);
        blogRepository.findAll().forEach(blog -> {
            if (blog.getTags().contains(tag.getId())) {
                blog.getTags().remove(tag.getId());
                blogRepository.save(blog);
            }
        });
        return ResponseEntity.ok("{\"success\": true}");
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
}
