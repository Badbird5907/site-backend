package dev.badbird.backend.util.markdown;

import dev.badbird.backend.model.Blog;
import dev.badbird.backend.object.GithubReference;
import dev.badbird.markdown.MarkdownParser;
import dev.badbird.markdown.object.TempMarkConfig;
import lombok.Data;

@Data
public class TempMarkWrapper {
    private final Blog blog;
    private final MarkdownParser markdownParser = new MarkdownParser(new TempMarkConfig()
            .setUrlMutator(this::setGithubURLs));

    public String getContents() {
        String s = blog.getWebContents();
        if (s == null) {
            return "Error: Unable to find content!";
        }
        System.out.println("Parsing: " + s);
        return markdownParser.parse(s);
    }

    public String setGithubURLs(String s) {
        if (blog.getLocation().getGithubReference() != null) {
            if (s.startsWith("{{import ")) {
                String imp = s.substring(9, s.length() - 2);
                if (!imp.startsWith("https://") && !imp.startsWith("http://")) {
                    GithubReference githubReference = blog.getLocation().getGithubReference();
                    String base = githubReference.getFullURLForDir();
                    if (!imp.startsWith("/")) {
                        imp = "/" + imp;
                    }
                    return "{{import " + base + imp + "}}";
                }
            }
        }
        return s;
    }
}
