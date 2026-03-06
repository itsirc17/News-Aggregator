import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true) // Ignoră câmpuri extra din JSON dacă apar
public class Article {
    public String uuid;
    public String title;
    public String author;
    public String url;
    public String text;
    public String published;
    public String language;
    public List<String> categories;

    public Article() {}

    public String getUuid() {
        return uuid;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getCategories() {
        return categories;
    }

    public String getLanguage() {
        return language;
    }

    public String getPublished() {
        return published;
    }

    public String getText() {
        return text;
    }

    public String getAuthor() {
        return author;
    }

    public String getUrl() {
        return url;
    }
}