package dev.badbird.backend.object;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GithubReferenceTest {
    @Test
    void testEffectiveURL() {
        GithubReference ref = new GithubReference("Badbird5907", "blog", "master", "/content/hi", "hi.md");
        String baseURL = ref.getEffectiveURL(false);
        //    private static final String GITHUB_BASE = "https://github.com/%USER%/%REPO%/%BRANCH%/%PATH%";
        assertEquals("https://github.com/Badbird5907/blog/master/content/hi/hi.md", baseURL);
        String rawURL = ref.getEffectiveURL(true);
        //    private static final String GITHUB_RAW_BASE = "https://raw.githubusercontent.com/%USER%/%REPO%/%BRANCH%/%PATH%";
        assertEquals("https://raw.githubusercontent.com/Badbird5907/blog/master/content/hi/hi.md", rawURL);
    }

    @Test
    void testDir() {
        GithubReference ref = new GithubReference("Badbird5907", "blog", "master", "/content/hi", "hi.md");
        String dir = ref.getFullURLForDir();
        assertEquals("https://github.com/Badbird5907/blog/master/content/hi", dir);
    }

    @Test
    void fromURL() {
        String url = "https://github.com/Badbird5907/blog/blob/master/content/test/Test.md";
        GithubReference ref = GithubReference.fromURL(url);
        assertEquals("Badbird5907", ref.getOwner());
        assertEquals("blog", ref.getRepo());
        assertEquals("master", ref.getBranch());
        assertEquals("content/test", ref.getDir());
        assertEquals("Test.md", ref.getFile());
    }


}
