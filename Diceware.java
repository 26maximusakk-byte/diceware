// Diceware.java
import java.io.*;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Diceware {
    private static final String[] DEFAULT_WORDS = {
        "a", "able", "about", "above", "abstract", "academy", "accept", "access", "account", "achieve",
        "across", "act", "action", "active", "activity", "actual", "adapt", "add", "address", "adjust",
        // ... (полный список)
        "zone", "zoo"
    };

    private List<String> wordList;
    private int wordCount;
    private String separator;
    private boolean addDigits;
    private boolean addSymbols;
    private boolean capitalize;
    private boolean uppercase;
    private SecureRandom random = new SecureRandom();

    public Diceware(int words, String sep, boolean digits, boolean symbols,
                    boolean cap, boolean up, String dictFile) throws IOException {
        this.wordCount = words;
        this.separator = sep;
        this.addDigits = digits;
        this.addSymbols = symbols;
        this.capitalize = cap;
        this.uppercase = up;
        this.wordList = loadDictionary(dictFile);
        if (wordList.size() < 7776) {
            System.err.println("Warning: word list has fewer than 7776 words, entropy may be lower.");
        }
    }

    private List<String> loadDictionary(String filepath) throws IOException {
        if (filepath != null && Files.exists(Paths.get(filepath))) {
            List<String> words = Files.readAllLines(Paths.get(filepath));
            words.removeIf(String::isEmpty);
            if (!words.isEmpty()) return words;
        }
        return Arrays.asList(DEFAULT_WORDS);
    }

    private int randomInt(int max) {
        return random.nextInt(max);
    }

    public Map<String, Object> generate() {
        List<String> selected = new ArrayList<>();
        int dictSize = wordList.size();
        for (int i = 0; i < wordCount; i++) {
            String word = wordList.get(randomInt(dictSize));
            if (capitalize) {
                word = word.substring(0, 1).toUpperCase() + word.substring(1);
            } else if (uppercase) {
                word = word.toUpperCase();
            }
            selected.add(word);
        }
        String password = String.join(separator, selected);
        if (addDigits) {
            int d1 = randomInt(10);
            int d2 = randomInt(10);
            password += d1 + "" + d2;
        }
        if (addSymbols) {
            String symbols = "!@#$%^&*()_+-=";
            char s1 = symbols.charAt(randomInt(symbols.length()));
            char s2 = symbols.charAt(randomInt(symbols.length()));
            password += s1 + "" + s2;
        }
        double entropy = calculateEntropy();
        Map<String, Object> result = new HashMap<>();
        result.put("password", password);
        result.put("entropy", entropy);
        return result;
    }

    private double calculateEntropy() {
        int dictSize = wordList.size();
        double bits = Math.log(dictSize) / Math.log(2) * wordCount;
        if (addDigits) bits += Math.log(10) / Math.log(2) * 2;
        if (addSymbols) bits += Math.log("!@#$%^&*()_+-=".length()) / Math.log(2) * 2;
        return bits;
    }

    public static void main(String[] args) throws Exception {
        int words = 6;
        String separator = " ";
        boolean digits = false, symbols = false, capitalize = false, uppercase = false;
        String dictFile = null, outputFile = null;
        boolean jsonOut = false, entropy = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--words": words = Integer.parseInt(args[++i]); break;
                case "--separator": separator = args[++i]; break;
                case "--digits": digits = true; break;
                case "--symbols": symbols = true; break;
                case "--capitalize": capitalize = true; break;
                case "--uppercase": uppercase = true; break;
                case "--dictionary": dictFile = args[++i]; break;
                case "--output": outputFile = args[++i]; break;
                case "--json": jsonOut = true; break;
                case "--entropy": entropy = true; break;
            }
        }

        Diceware gen = new Diceware(words, separator, digits, symbols, capitalize, uppercase, dictFile);
        Map<String, Object> result = gen.generate();
        String password = (String) result.get("password");
        double ent = (double) result.get("entropy");

        if (jsonOut) {
            Map<String, Object> out = new HashMap<>();
            out.put("password", password);
            if (entropy) out.put("entropy", ent);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            System.out.println(gson.toJson(out));
        } else {
            System.out.println("Password: \"" + password + "\"");
            if (entropy) {
                System.out.printf("Entropy: %.2f bits\n", ent);
            }
        }

        if (outputFile != null) {
            Files.write(Paths.get(outputFile), password.getBytes());
            System.out.println("Password saved to " + outputFile);
        }
    }
}
