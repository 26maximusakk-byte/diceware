// diceware.cpp
#include <iostream>
#include <fstream>
#include <vector>
#include <string>
#include <random>
#include <cmath>
#include <cstring>
#include <algorithm>
#include <chrono>
#include <nlohmann/json.hpp> // requires nlohmann/json

using namespace std;
using json = nlohmann::json;

// Встроенный словарь (урезанный)
const vector<string> DEFAULT_WORDS = {
    "a", "able", "about", "above", "abstract", "academy", "accept", "access", "account", "achieve",
    "across", "act", "action", "active", "activity", "actual", "adapt", "add", "address", "adjust",
    // ... (полный список)
    "zone", "zoo"
};

vector<string> load_dictionary(const string& filepath) {
    if (!filepath.empty()) {
        ifstream file(filepath);
        if (file) {
            vector<string> words;
            string line;
            while (getline(file, line)) {
                if (!line.empty()) {
                    // убираем пробелы
                    line.erase(0, line.find_first_not_of(" \t"));
                    line.erase(line.find_last_not_of(" \t") + 1);
                    if (!line.empty())
                        words.push_back(line);
                }
            }
            if (!words.empty()) return words;
        }
    }
    return DEFAULT_WORDS;
}

class Generator {
public:
    Generator(int words, const string& sep, bool digits, bool symbols,
              bool cap, bool up, const string& dictFile)
        : wordCount(words), separator(sep), addDigits(digits),
          addSymbols(symbols), capitalize(cap), uppercase(up) {
        wordList = load_dictionary(dictFile);
        if (wordList.size() < 7776) {
            cerr << "Warning: word list has fewer than 7776 words, entropy may be lower." << endl;
        }
        // Инициализация генератора случайных чисел
        unsigned seed = chrono::steady_clock::now().time_since_epoch().count();
        rng.seed(seed);
    }

    pair<string, double> generate() {
        vector<string> selected;
        int dictSize = wordList.size();
        uniform_int_distribution<int> dist(0, dictSize - 1);
        for (int i = 0; i < wordCount; ++i) {
            string word = wordList[dist(rng)];
            if (capitalize) {
                if (!word.empty()) {
                    word[0] = toupper(word[0]);
                }
            } else if (uppercase) {
                transform(word.begin(), word.end(), word.begin(), ::toupper);
            }
            selected.push_back(word);
        }
        string password = join(selected, separator);
        if (addDigits) {
            password += to_string(dist(rng) % 10) + to_string(dist(rng) % 10);
        }
        if (addSymbols) {
            string symbols = "!@#$%^&*()_+-=";
            password += symbols[dist(rng) % symbols.size()];
            password += symbols[dist(rng) % symbols.size()];
        }
        double entropy = calculate_entropy();
        return {password, entropy};
    }

private:
    string join(const vector<string>& parts, const string& sep) {
        string result;
        for (size_t i = 0; i < parts.size(); ++i) {
            if (i > 0) result += sep;
            result += parts[i];
        }
        return result;
    }

    double calculate_entropy() {
        int dictSize = wordList.size();
        double bits = log2(dictSize) * wordCount;
        if (addDigits) bits += log2(10) * 2;
        if (addSymbols) bits += log2(string("!@#$%^&*()_+-=").size()) * 2;
        return bits;
    }

    int wordCount;
    string separator;
    bool addDigits;
    bool addSymbols;
    bool capitalize;
    bool uppercase;
    vector<string> wordList;
    mt19937 rng;
};

int main(int argc, char* argv[]) {
    int words = 6;
    string separator = " ";
    bool digits = false, symbols = false, capitalize = false, uppercase = false;
    string dictFile, outputFile;
    bool jsonOut = false, entropy = false;

    for (int i = 1; i < argc; ++i) {
        string arg = argv[i];
        if (arg == "--words" && i+1 < argc) words = stoi(argv[++i]);
        else if (arg == "--separator" && i+1 < argc) separator = argv[++i];
        else if (arg == "--digits") digits = true;
        else if (arg == "--symbols") symbols = true;
        else if (arg == "--capitalize") capitalize = true;
        else if (arg == "--uppercase") uppercase = true;
        else if (arg == "--dictionary" && i+1 < argc) dictFile = argv[++i];
        else if (arg == "--output" && i+1 < argc) outputFile = argv[++i];
        else if (arg == "--json") jsonOut = true;
        else if (arg == "--entropy") entropy = true;
    }

    Generator gen(words, separator, digits, symbols, capitalize, uppercase, dictFile);
    auto result = gen.generate();
    string password = result.first;
    double ent = result.second;

    if (jsonOut) {
        json j;
        j["password"] = password;
        if (entropy) j["entropy"] = ent;
        cout << j.dump(2) << endl;
    } else {
        cout << "Password: \"" << password << "\"" << endl;
        if (entropy) {
            cout << "Entropy: " << fixed << setprecision(2) << ent << " bits" << endl;
        }
    }

    if (!outputFile.empty()) {
        ofstream out(outputFile);
        if (out) {
            out << password;
            cout << "Password saved to " << outputFile << endl;
        } else {
            cerr << "Error: could not write to " << outputFile << endl;
        }
    }
    return 0;
}
