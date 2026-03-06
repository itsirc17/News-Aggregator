import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MyData {
    public int P;
    public CyclicBarrier barrier;

    public List<String> jsonFiles;
    public AtomicInteger fileIndex = new AtomicInteger(0);

    public List<Article> rawArticles = Collections.synchronizedList(new ArrayList<>());
    public List<Article> cleanArticles = Collections.synchronizedList(new ArrayList<>());
    public ConcurrentHashMap<String, Integer> uuidCounts = new ConcurrentHashMap<>();
    public ConcurrentHashMap<String, Integer> titleCounts = new ConcurrentHashMap<>();

    // folosite pentru fisierele de la categories, languages si words
    public List<String> targetCategories;
    public ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> mapCategories = new ConcurrentHashMap<>();
    public List<String> targetLanguages;
    public ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> mapLanguages = new ConcurrentHashMap<>();
    public Set<String> targetLinking_words = new HashSet<>();
    public ConcurrentHashMap<String, Integer> keywordCounts = new ConcurrentHashMap<>();

    // Folosite la statistici la final
    public ConcurrentHashMap<String, Integer> authorCounts = new ConcurrentHashMap<>();
    public ConcurrentHashMap<String, Integer> languageCounts = new ConcurrentHashMap<>();
    public ConcurrentHashMap<String, Integer> categoryCounts = new ConcurrentHashMap<>();

    public String articlesFilePath;
    public String auxFilePath;

    public MyData(int P, List<String> jsonFiles, List<String> auxFiles, String articlesFilePath, String auxFilePath) {
        this.P = P;
        this.jsonFiles = jsonFiles;
        this.barrier = new CyclicBarrier(P);

        this.articlesFilePath = articlesFilePath;
        this.auxFilePath = auxFilePath;
        this.targetCategories = new ArrayList<>();
        this.targetLanguages = new ArrayList<>();
    }
}