// Diceware.kt
import java.io.File
import java.security.SecureRandom
import kotlin.math.log2
import com.google.gson.GsonBuilder

val DEFAULT_WORDS = listOf(
    "a", "able", "about", "above", "abstract", "academy", "accept", "access", "account", "achieve",
    "across", "act", "action", "active", "activity", "actual", "adapt", "add", "address", "adjust",
    // ... (полный список)
    "zone", "zoo"
)

fun loadDictionary(filepath: String?): List<String> {
    if (filepath != null) {
        val file = File(filepath)
        if (file.exists()) {
            val lines = file.readLines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.isNotEmpty()) return lines
        }
    }
    return DEFAULT_WORDS
}

class Generator(
    private val wordCount: Int,
    private val separator: String,
    private val addDigits: Boolean,
    private val addSymbols: Boolean,
    private val capitalize: Boolean,
    private val uppercase: Boolean,
    dictionaryFile: String?
) {
    private val wordList = loadDictionary(dictionaryFile)
    private val random = SecureRandom()

    init {
        if (wordList.size < 7776) {
            System.err.println("Warning: word list has fewer than 7776 words, entropy may be lower.")
        }
    }

    fun generate(): Pair<String, Double> {
        val selected = mutableListOf<String>()
        val dictSize = wordList.size
        repeat(wordCount) {
            var word = wordList[random.nextInt(dictSize)]
            when {
                capitalize -> word = word.replaceFirstChar { it.uppercase() }
                uppercase -> word = word.uppercase()
            }
            selected.add(word)
        }
        var password = selected.joinToString(separator)
        if (addDigits) {
            val d1 = random.nextInt(10)
            val d2 = random.nextInt(10)
            password += "$d1$d2"
        }
        if (addSymbols) {
            val symbols = "!@#$%^&*()_+-="
            val s1 = symbols[random.nextInt(symbols.length)]
            val s2 = symbols[random.nextInt(symbols.length)]
            password += "$s1$s2"
        }
        val entropy = calculateEntropy()
        return Pair(password, entropy)
    }

    private fun calculateEntropy(): Double {
        val dictSize = wordList.size
        var bits = log2(dictSize.toDouble()) * wordCount
        if (addDigits) bits += log2(10.0) * 2
        if (addSymbols) bits += log2("!@#$%^&*()_+-=".length.toDouble()) * 2
        return bits
    }
}

fun main(args: Array<String>) {
    var words = 6
    var separator = " "
    var digits = false
    var symbols = false
    var capitalize = false
    var uppercase = false
    var dictFile: String? = null
    var outputFile: String? = null
    var jsonOut = false
    var showEntropy = false

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--words" -> words = args[++i].toInt()
            "--separator" -> separator = args[++i]
            "--digits" -> digits = true
            "--symbols" -> symbols = true
            "--capitalize" -> capitalize = true
            "--uppercase" -> uppercase = true
            "--dictionary" -> dictFile = args[++i]
            "--output" -> outputFile = args[++i]
            "--json" -> jsonOut = true
            "--entropy" -> showEntropy = true
        }
        i++
    }

    val gen = Generator(words, separator, digits, symbols, capitalize, uppercase, dictFile)
    val (password, entropy) = gen.generate()

    if (jsonOut) {
        val map = mutableMapOf<String, Any>("password" to password)
        if (showEntropy) map["entropy"] = entropy
        val gson = GsonBuilder().setPrettyPrinting().create()
        println(gson.toJson(map))
    } else {
        println("Password: \"$password\"")
        if (showEntropy) {
            println("Entropy: ${String.format("%.2f", entropy)} bits")
        }
    }

    if (outputFile != null) {
        File(outputFile).writeText(password)
        println("Password saved to $outputFile")
    }
}
