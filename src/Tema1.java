import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

public class Tema1 {
    public static void main(String[] args) {
        if (args.length < 3) {
            return;
        }
        int P = Integer.parseInt(args[0]);
        String articlesFilePath = args[1];
        String auxFilePath = args[2];

        List<String> inputJsonFiles = readLines(articlesFilePath);
        if (!inputJsonFiles.isEmpty()) inputJsonFiles.remove(0);

        List<String> auxFiles = readLines(auxFilePath);
        if (!auxFiles.isEmpty()) auxFiles.remove(0);

        String categoriesPath = null;
        String languagesPath = null;
        String linking_wordsPath = null;
        for (String path : auxFiles) {
            if (path.endsWith("categories.txt")) {
                File inputsFile = new File(auxFilePath);
                String parentDir = inputsFile.getParent();
                if (parentDir == null){
                    parentDir = ".";
                }

                categoriesPath = new File(parentDir, path).getPath();
            }
            if (path.endsWith("languages.txt")) {
                File inputsFile = new File(auxFilePath);
                String parentDir = inputsFile.getParent();
                if (parentDir == null){
                    parentDir = ".";
                }

                languagesPath = new File(parentDir, path).getPath();
            }
            if (path.endsWith("english_linking_words.txt")) {
                File inputsFile = new File(auxFilePath);
                String parentDir = inputsFile.getParent();
                if (parentDir == null){
                    parentDir = ".";
                }

                linking_wordsPath = new File(parentDir, path).getPath();
            }
        }

        MyData data = new MyData(P, inputJsonFiles, auxFiles, articlesFilePath, auxFilePath);

        data.targetCategories = readLines(categoriesPath);
        if (!data.targetCategories.isEmpty()) data.targetCategories.remove(0);

        data.targetLanguages = readLines(languagesPath);
        if (!data.targetLanguages.isEmpty()) data.targetLanguages.remove(0);

        List<String> linking_wordsList = readLines(linking_wordsPath);
        if (!linking_wordsList.isEmpty()) linking_wordsList.remove(0);
        data.targetLinking_words = new HashSet<>(linking_wordsList);

        Thread[] threads = new Thread[P];
        for (int i = 0; i < P; i++) {
            threads[i] = new Thread(new Worker(i, data));
            threads[i].start();
        }

        for (int i = 0; i < P; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static List<String> readLines(String path) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            do{
                line = br.readLine();
                if (line != null){
                    lines.add(line.trim());
                }
            }while(line != null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }
}

class Worker implements Runnable {
    private int id;
    private MyData data;

    public Worker(int id, MyData data) {
        this.id = id;
        this.data = data;
    }

    @Override
    public void run() {
        try {
            // Citirea datelor------------------------------
            ObjectMapper mapper = new ObjectMapper();
            while (true) {
                int idx = data.fileIndex.getAndIncrement();

                if (idx >= data.jsonFiles.size()) {
                    break;
                }
                // Ierarhia catre articolul current, din fisierul de test
                String filenameRelative = data.jsonFiles.get(idx);

                // Calea catre articles.txt
                File inputsFile = new File(data.articlesFilePath);

                // Extragem doar folderul parinte (exemplu: checker/input/tests/test_small)
                String parentDir = inputsFile.getParent();
                if (parentDir == null) {
                    parentDir = ".";
                }

                // Le combinam
                File infile = new File(parentDir, filenameRelative);
                String filename = infile.getPath();

                File file = new File(filename);

                try {
                    List<Article> articles = mapper.readValue(file, new TypeReference<List<Article>>(){});
                    data.rawArticles.addAll(articles);
                } catch (Exception e) {
                }
            }

            data.barrier.await();

            //Eliminare duplicate---------------------------------------------------------
            int totalArticles = data.rawArticles.size();

            int chunkSize = totalArticles / data.P;
            int start = id * chunkSize;
            int end = (id == data.P - 1) ? totalArticles : (id + 1) * chunkSize;

            // Adaugam articolele in hashmap uri(contorizare id si titlu)
            for (int i = start; i < end; i++) {
                Article art = data.rawArticles.get(i);

                data.uuidCounts.merge(art.getUuid(), 1, Integer::sum);
                data.titleCounts.merge(art.getTitle(), 1, Integer::sum);
            }

            data.barrier.await();

            // Adaugam in cleanArticles doar ce nu e duplicat
            for (int i = start; i < end; i++){
                Article art = data.rawArticles.get(i);
                Integer countUuid = data.uuidCounts.get(art.getUuid());
                Integer countTitle = data.titleCounts.get(art.getTitle());

                if (countUuid != null && countUuid == 1 && countTitle != null && countTitle == 1) {
                    data.cleanArticles.add(art);
                }
            }

            data.barrier.await();

            // Salvam doar categoriile limbile si articolele respective care trebuie adaugate in fisiere

            int cleanSize = data.cleanArticles.size();
            int chunkClean = cleanSize / data.P;
            start = id * chunkClean;
            end = (id == data.P - 1) ? cleanSize : (id + 1) * chunkClean;

            Set<String> validCategoriesSet = new HashSet<>(data.targetCategories);
            Set<String> validLanguagesSet = new HashSet<>(data.targetLanguages);

            for (int i = start; i < end; i++) {
                Article art = data.cleanArticles.get(i);
                List<String> artCategs = art.getCategories();
                String artLanguage = art.getLanguage();

                // Categories
                if (artCategs != null) {
                    Set<String> uniqueArtCats = new HashSet<>(artCategs);

                    for (String cat : uniqueArtCats) {
                        if (validCategoriesSet.contains(cat)) {
                            data.mapCategories.computeIfAbsent(cat, k -> new ConcurrentLinkedQueue<>()).add(art.getUuid());
                        }
                    }
                }

                // Languages
                if (artLanguage != null) {
                    if (validLanguagesSet.contains(artLanguage)) {
                        data.mapLanguages.computeIfAbsent(artLanguage, k -> new ConcurrentLinkedQueue<>()).add(art.getUuid());
                    }
                }
            }

            data.barrier.await();

            // Generam fisierele de categorii si limbi

            List<String> categoriesToProcess = new ArrayList<>(data.mapCategories.keySet());
            int numCategsProcessing = categoriesToProcess.size();

            for (int i = id; i < numCategsProcessing; i += data.P) {
                String category = categoriesToProcess.get(i);
                ConcurrentLinkedQueue<String> queue = data.mapCategories.get(category);

                // Convertim la list pentru sortare
                List<String> soretedUuids = new ArrayList<>(queue);
                Collections.sort(soretedUuids);

                // Construim numele fisierului
                category = category.replace(",", "").replace(" ", "_");
                String filename = category + ".txt";

                try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(filename)))) {
                    for (String uuid : soretedUuids) {
                        pw.println(uuid);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            List<String> languagesToProcess = new ArrayList<>(data.mapLanguages.keySet());
            int numLangssProcessing = languagesToProcess.size();

            for (int i = id; i < numLangssProcessing; i += data.P) {
                String language = languagesToProcess.get(i);
                ConcurrentLinkedQueue<String> queue = data.mapLanguages.get(language);

                // Convertim la list pentru sortare
                List<String> soretedUuids = new ArrayList<>(queue);
                Collections.sort(soretedUuids);

                // Construim numele fisierului
                String filename = language + ".txt";

                try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(filename)))) {
                    for (String uuid : soretedUuids) {
                        pw.println(uuid);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            data.barrier.await();

            // Partea de statistici si keywords_count
            for (int i = start; i < end; i++) {
                Article art = data.cleanArticles.get(i);

                data.authorCounts.merge(art.getAuthor(), 1, Integer::sum);
                data.languageCounts.merge(art.getLanguage(), 1, Integer::sum);

                if (art.getCategories() != null) {
                    // Creem un set in cazul in care un articol avea aceasi categorie de mai multe ori
                    // (sau poate au adaugat 2 thread uri diferite aceasi categorie)
                    Set<String> uniqueCats = new HashSet<>(art.getCategories());
                    for (String cat : uniqueCats) {
                        data.categoryCounts.merge(cat, 1, Integer::sum);
                    }
                }

                if ("english".equals(art.getLanguage())) {
                    String text = art.getText();

                    String[] words = text.toLowerCase().split("\\s+"); // ltiera mica si despartit de spatiu
                    Set<String> uniqueWords = new HashSet<>();

                    for (String word : words) {
                        String cleanWord = word.replaceAll("[^a-z]", ""); // ce nu e litera scoatem
                        if (!cleanWord.isEmpty() && !data.targetLinking_words.contains(cleanWord)) {
                            uniqueWords.add(cleanWord);
                        }
                    }
                    for (String distinctWord : uniqueWords) {
                        data.keywordCounts.merge(distinctWord, 1, Integer::sum);
                    }
                }
            }

            data.barrier.await();

            // PARTEA DE SCRIERE IN FISIERE

            // keywords_count.txt
            if (id == 0) {
                List<Map.Entry<String, Integer>> sortedKeywords = new ArrayList<>(data.keywordCounts.entrySet());
                sortedKeywords.sort(new Comparator<Map.Entry<String, Integer>>() {
                    @Override
                    public int compare(Map.Entry<String, Integer> e1, Map.Entry<String, Integer> e2) {
                        if (e2.getValue().compareTo(e1.getValue()) != 0) {
                            return e2.getValue().compareTo(e1.getValue());
                        }
                        return e1.getKey().compareTo(e2.getKey());
                    }
                });
                try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter("keywords_count.txt")))) {
                    for (Map.Entry<String, Integer> entry : sortedKeywords) {
                        pw.println(entry.getKey() + " " + entry.getValue());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // all_articles.txt
            if (id == 0) {
                data.cleanArticles.sort(new Comparator<Article>() {
                    @Override
                    public int compare(Article a1, Article a2) {
                        String date1 = a1.getPublished();
                        String date2 = a2.getPublished();
                        if (date2.compareTo(date1) != 0) {
                            return date2.compareTo(date1);
                        }
                        return a1.getUuid().compareTo(a2.getUuid());
                    }
                });
                try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter("all_articles.txt")))) {
                    for (Article art : data.cleanArticles) {
                        pw.println(art.getUuid() + " " + art.getPublished());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // reports.txt
            if(id == 0) {
                try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter("reports.txt")))) {

                    int duplicates = data.rawArticles.size() - data.cleanArticles.size();
                    pw.println("duplicates_found - " + duplicates);

                    pw.println("unique_articles - " + data.cleanArticles.size());

                    String bestAuthor = getTopKey(data.authorCounts);
                    pw.println("best_author - " + bestAuthor);

                    String topLang = getTopKey(data.languageCounts);
                    pw.println("top_language - " + topLang);

                    String topCat = getTopKey(data.categoryCounts);
                    pw.println("top_category - " + topCat);

                    Article mostRecent = data.cleanArticles.get(0);
                    pw.println("most_recent_article - " + mostRecent.getPublished() + " " + mostRecent.getUrl());

                    String topKeyword = getTopKey(data.keywordCounts);
                    pw.println("top_keyword_en - " + topKeyword);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }



        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getTopKey(ConcurrentHashMap<String, Integer> map) {
        String bestKey = "";
        int maxVal = -1;

        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            String currentKey = entry.getKey();
            int currentVal = entry.getValue();

            if (currentVal > maxVal) {
                maxVal = currentVal;
                bestKey = currentKey;
            } else if (currentVal == maxVal) {
                if (bestKey.equals("") || currentKey.compareTo(bestKey) < 0) {
                    bestKey = currentKey;
                }
            }
        }

        int count = map.get(bestKey);
        //Caz special de nume la categories(trebuie cu inlocuire de ',' si ' ' ca atunci cand generezi fisieru)
        if(data.mapCategories.containsKey(bestKey)) {
            bestKey = bestKey.replace(",", "").replace(" ", "_");
        }
        return bestKey + " " + count;
    }
}