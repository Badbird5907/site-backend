package dev.badbird.backend.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mongodb.client.MongoClient;
import dev.badbird.backend.model.Blog;
import dev.badbird.backend.model.Tag;
import dev.badbird.backend.model.User;
import dev.badbird.backend.object.Location;
import dev.badbird.backend.repositories.BlogRepository;
import dev.badbird.backend.repositories.TagsRepository;
import dev.badbird.backend.repositories.UserRepository;
import dev.badbird.backend.util.Utils;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequestMapping("/blog")
@RestController
@CrossOrigin(origins = "*")
public class BlogController {
    private static final String BLOG_NOT_FOUND = "{\"success\": false, \"error\": \"Blog not found\", \"code\": 404}";
    @Autowired
    private BlogRepository blogRepository;
    @Autowired
    private Gson gson;

    @SneakyThrows
    @GetMapping("/meta/get/{id}")
    public ResponseEntity<?> getBlogInfo(@PathVariable("id") String idStr) {
        idStr = URLDecoder.decode(idStr, StandardCharsets.UTF_8.toString());
        Optional<Blog> optionalBlog = blogRepository.findById(idStr);
        if (!optionalBlog.isPresent()) {
            optionalBlog = blogRepository.findByTitle(idStr);
            if (!optionalBlog.isPresent()) {
                return ResponseEntity.status(404)
                        .body(BLOG_NOT_FOUND);
            }
        }
        return ResponseEntity.ok(gson.toJson(getBlogMeta(optionalBlog)));
    }

    @SneakyThrows
    @GetMapping("/content/get/{id}") // More expensive, so it's a separate endpoint
    public ResponseEntity<?> getBlogContent(@PathVariable("id") String idStr) {
        idStr = URLDecoder.decode(idStr, StandardCharsets.UTF_8.toString());
        Optional<Blog> optionalBlog = blogRepository.findById(idStr);
        if (!optionalBlog.isPresent()) {
            optionalBlog = blogRepository.findByTitle(idStr);
            if (!optionalBlog.isPresent()) {
                return ResponseEntity.status(404)
                        .body(BLOG_NOT_FOUND);
            }
        }
        try {
            JsonObject data = getBlogMeta(optionalBlog);
            data.addProperty("content", optionalBlog.get().getContent());
            return ResponseEntity.ok(gson.toJson(data));
        } catch (Exception e) {
            String githubURL;
            Location location = optionalBlog.get().getLocation();
            e.printStackTrace();
            if (location.getGithubReference() != null) {
                githubURL = location.getGithubReference().getEffectiveURL(false);
                return ResponseEntity.status(500)
                        .body("{\"success\": false, \"error\": \"Error getting blog content\", \"code\": 500, \"githubURL\": \"" + githubURL + "\"}");
            }
            return ResponseEntity.status(500)
                    .body("{\"success\": false, \"error\": \"Internal server error\", \"code\": 500}");
        }
    }

    @Autowired
    private MongoClient mongoClient;
    @Autowired
    private TagsRepository tagsRepository;
    private static final String CONTAINS_PATTERN = ".*%s.*";

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    @SneakyThrows
    @GetMapping("/list")
    public ResponseEntity<?> getBlogList(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "15") int size,
            @RequestParam(value = "order", defaultValue = "asc") String order,
            @SuppressWarnings("unused") @RequestParam(value = "sort", defaultValue = "timestamp") String sort, // Timestamp is the only valid option for now
            @RequestParam(value = "search", defaultValue = "") String search,
            @RequestParam(value = "tags", defaultValue = "") String tags,
            @RequestParam(value = "author", defaultValue = "") String author
    ) {
        if (size > 200) {
            return ResponseEntity.badRequest().body("{\"success\": false, \"error\": \"Size too large\", \"max\": 200}");
        }
        if (order.isEmpty()) order = "asc";

        String realOrder = order.toLowerCase();
        if (!realOrder.equals("asc") && !realOrder.equals("desc")) {
            return ResponseEntity.badRequest().body("{\"success\": false, \"error\": \"Invalid order, valid: asc and desc\"}");
        }
        Sort.Direction direction = realOrder.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable p = PageRequest.of(page - 1, size, Sort.by(direction, "timestamp")); // Hardcoded timestamp for now because it's the only valid option, change later
        List<Blog> blogs;
        Query query = new Query();
        MongoTemplate mongoTemplate = new MongoTemplate(mongoClient, databaseName);
        if (!search.isEmpty()) {
            String str = String.format(CONTAINS_PATTERN, Utils.escapeRegex(search));
            // either title or description contains the search string
            query.addCriteria(Criteria.where("title").regex(str, "i") // TODO not the best solution using regex
                    .orOperator(Criteria.where("description").regex(str, "i")));
        }
        if (!tags.isEmpty()) {
            String[] tagsArray = tags.split(",");
            List<String> tagsList = new ArrayList<>(); // a list of tag ids
            for (String tag : tagsArray) {
                tag = URLDecoder.decode(tag, StandardCharsets.UTF_8);
                Optional<Tag> optionalTag = tagsRepository.findByName(tag);
                optionalTag.ifPresent(t -> tagsList.add(t.getId()));
            }
            query.addCriteria(Criteria.where("tags").all(tagsList));
        }
        if (!author.isEmpty()) {
            author = URLDecoder.decode(author, StandardCharsets.UTF_8);
            query.addCriteria(Criteria.where("author").is(author)
                    .orOperator(
                            Criteria.where("authorId").is(author)
                    ));
        }
        query.with(p);
        blogs = mongoTemplate.find(query, Blog.class, "blogs");
        JsonObject jsonObject = new JsonObject();
        List<JsonObject> blogList = new ArrayList<>();
        for (Blog blog : blogs) {
            blogList.add(getBlogMeta(Optional.of(blog)));
        }
        Query countQuery = new Query();
        if (!search.isEmpty()) {
            String str = String.format(CONTAINS_PATTERN, Utils.escapeRegex(search));
            // either title or description contains the search string
            countQuery.addCriteria(Criteria.where("title").regex(str, "i") // TODO down here too
                    .orOperator(Criteria.where("description").regex(str, "i")));
        }
        if (!tags.isEmpty()) {
            String[] tagsArray = tags.split(",");
            List<String> tagsList = new ArrayList<>(); // a list of tag ids
            for (String tag : tagsArray) {
                tag = URLDecoder.decode(tag, StandardCharsets.UTF_8);
                Optional<Tag> optionalTag = tagsRepository.findByName(tag);
                optionalTag.ifPresent(t -> tagsList.add(t.getId()));
            }
            countQuery.addCriteria(Criteria.where("tags").all(tagsList));
        }
        if (!author.isEmpty()) {
            author = URLDecoder.decode(author, StandardCharsets.UTF_8);
            countQuery.addCriteria(Criteria.where("author").is(author));
        }

        int count = (int) mongoTemplate.count(countQuery, Blog.class, "blogs");
        jsonObject.add("blogs", gson.toJsonTree(blogList));
        jsonObject.addProperty("success", true);
        jsonObject.addProperty("page", page);
        jsonObject.addProperty("size", size);
        jsonObject.addProperty("total", count);
        int totalPages = (int) Math.ceil((double) count / size);
        jsonObject.addProperty("totalPages", totalPages);

        return ResponseEntity.ok(gson.toJson(jsonObject));
    }

    public JsonObject getBlogMeta(Optional<Blog> optionalBlog) {
        return getBlogMeta(optionalBlog.get());
    }

    @Autowired
    private UserRepository userRepository;

    public JsonObject getBlogMeta(Blog blog) {
        JsonObject jsonObject = gson.toJsonTree(blog)
                .getAsJsonObject();
        jsonObject.addProperty("success", true);
        Optional<User> author = userRepository.findById(blog.getAuthorId());
        if (author.isPresent()) {
            jsonObject.addProperty("author", author.get().getUsername());
            jsonObject.addProperty("authorImg", author.get().getImageUrl());
        } else {
            jsonObject.addProperty("author", "Deleted User");
            jsonObject.addProperty("authorImg", User.DEFAULT_PROFILE);
        }
        jsonObject.addProperty("safeName", blog.getURLSafeTitle());
        jsonObject.remove("cached");
        return jsonObject;
    }

}
