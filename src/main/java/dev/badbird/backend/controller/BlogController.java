package dev.badbird.backend.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mongodb.client.MongoClient;
import dev.badbird.backend.model.Blog;
import dev.badbird.backend.model.Tag;
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
import org.springframework.data.mongodb.core.query.MongoRegexCreator;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        idStr = URLDecoder.decode(idStr, StandardCharsets.UTF_8);
        Optional<Blog> optionalBlog = blogRepository.findById(idStr);
        if (optionalBlog.isEmpty()) {
            List<Blog> list = blogRepository.findByTitle(idStr);
            optionalBlog = list.stream().findFirst();
            if (optionalBlog.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(BLOG_NOT_FOUND);
            }
        }
        return ResponseEntity.ok(gson.toJson(getBlogMeta(optionalBlog)));
    }

    @SneakyThrows
    @GetMapping("/content/get/{id}") // More expensive, so it's a separate endpoint
    public ResponseEntity<?> getBlogContent(@PathVariable("id") String idStr) {
        idStr = URLDecoder.decode(idStr, StandardCharsets.UTF_8);
        Optional<Blog> optionalBlog = blogRepository.findById(idStr);
        if (optionalBlog.isEmpty()) { // TODO maybe implement a way to detect if there are blogs with the same title, if so, have the frontend use the id instead.
            List<Blog> list = blogRepository.findByTitle(idStr);
            optionalBlog = list.stream().findFirst();
            if (optionalBlog.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(BLOG_NOT_FOUND);
            }
        }
        JsonObject data = getBlogMeta(optionalBlog);
        try {
            data.addProperty("content", optionalBlog.get().getContent());
            return ResponseEntity.ok(gson.toJson(data));
        } catch (Exception e) {
            String githubURL;
            Location location = optionalBlog.get().getLocation();
            e.printStackTrace();
            if (location.getGithubReference() != null) {
                githubURL = location.getGithubReference().getEffectiveURL(false);
                JsonObject returnData = new JsonObject();
                returnData.addProperty("success", false);
                //return ResponseEntity.status(500)
                //        .body("{\"success\": false, \"error\": \"Error getting blog content\", \"code\": 500, \"githubURL\": \"" + githubURL + "\"}");
                returnData.addProperty("error", "Error getting blog content");
                returnData.addProperty("code", 500);
                returnData.addProperty("githubURL", githubURL);
                if (data != null) {
                    returnData.add("data", data);
                }
                return ResponseEntity.status(500)
                        .body(gson.toJson(returnData));
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
        Sort.Direction direction = realOrder.equals("asc") ? Sort.Direction.DESC : Sort.Direction.ASC; // Kinda weird but we're sorting by timestamp here.
        Pageable p = PageRequest.of(page - 1, size, Sort.by(direction, "timestamp")); // Hardcoded timestamp for now because it's the only valid option, change later
        List<Blog> blogs;
        Query query = new Query();
        MongoTemplate mongoTemplate = new MongoTemplate(mongoClient, databaseName);
        addSearchQuery(search, query);
        if (!tags.isEmpty()) {
            String[] tagsArray = tags.split(",");
            List<String> tagsList = new ArrayList<>(); // a list of tag ids
            for (String tag : tagsArray) {
                tag = URLDecoder.decode(tag, StandardCharsets.UTF_8);
                Optional<Tag> optionalTag = tagsRepository.findByName(tag);
                if (optionalTag.isEmpty()) {
                    optionalTag = tagsRepository.findById(tag);
                    if (optionalTag.isEmpty()) {
                        return ResponseEntity.badRequest().body("{\"success\": false, \"error\": \"Invalid tag: " + tag + "\"}");
                    }
                }
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
        addSearchQuery(search, countQuery);
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

    private void addSearchQuery(String search, Query query) {
        search = Utils.escapeRegex(search);
        if (!search.isEmpty()) {
            String str = MongoRegexCreator.INSTANCE.toRegularExpression(search, MongoRegexCreator.MatchMode.CONTAINING);
            if (str == null) {
                System.err.println("Error creating regex for search string: " + search);
                str = String.format(CONTAINS_PATTERN, Utils.escapeRegex(search));
            }
            query.addCriteria(Criteria.where("title").regex(str, "i")
                    .orOperator(Criteria.where("description").regex(str, "i")));
        }
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
        jsonObject.addProperty("author", blog.getAuthorName(userRepository));
        jsonObject.addProperty("authorImg", blog.getAuthorImage(userRepository));

        // check if user is logged in
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            jsonObject.addProperty("customAuthor", blog.hasCustomAuthor());
        }

        jsonObject.addProperty("safeName", blog.getURLSafeTitle());
        List<String> tagIDs = blog.getTags();
        List<Tag> tags = new ArrayList<>();
        if (tagIDs != null) {
            for (String tagID : tagIDs) {
                Optional<Tag> optionalTag = tagsRepository.findById(tagID);
                optionalTag.ifPresent(tags::add);
            }
        }
        jsonObject.add("tags", gson.toJsonTree(tags));
        jsonObject.remove("cached");
        jsonObject.addProperty("id", blog.getId());
        return jsonObject;
    }
}
