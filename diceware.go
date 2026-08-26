// diceware.go
package main

import (
	"bufio"
	"crypto/rand"
	"encoding/json"
	"flag"
	"fmt"
	"math"
	"math/big"
	"os"
	"strings"
)

// Встроенный словарь (урезанный)
var defaultWords = []string{
	"a", "able", "about", "above", "abstract", "academy", "accept", "access", "account", "achieve",
	"across", "act", "action", "active", "activity", "actual", "adapt", "add", "address", "adjust",
	// ... (полный список должен быть загружен)
	"zone", "zoo",
}

func loadDictionary(filepath string) []string {
	if filepath != "" {
		file, err := os.Open(filepath)
		if err == nil {
			defer file.Close()
			var words []string
			scanner := bufio.NewScanner(file)
			for scanner.Scan() {
				line := strings.TrimSpace(scanner.Text())
				if line != "" {
					words = append(words, line)
				}
			}
			if len(words) > 0 {
				return words
			}
		}
	}
	return defaultWords
}

type Generator struct {
	wordCount  int
	separator  string
	addDigits  bool
	addSymbols bool
	capitalize bool
	uppercase  bool
	wordList   []string
}

func NewGenerator(words int, sep string, digits, symbols, cap, up bool, dictFile string) *Generator {
	wordList := loadDictionary(dictFile)
	if len(wordList) < 7776 {
		fmt.Fprintln(os.Stderr, "Warning: word list has fewer than 7776 words, entropy may be lower.")
	}
	return &Generator{
		wordCount:  words,
		separator:  sep,
		addDigits:  digits,
		addSymbols: symbols,
		capitalize: cap,
		uppercase:  up,
		wordList:   wordList,
	}
}

func (g *Generator) randomInt(max int) int {
	n, err := rand.Int(rand.Reader, big.NewInt(int64(max)))
	if err != nil {
		panic(err)
	}
	return int(n.Int64())
}

func (g *Generator) Generate() (string, float64) {
	selected := make([]string, g.wordCount)
	dictSize := len(g.wordList)
	for i := 0; i < g.wordCount; i++ {
		idx := g.randomInt(dictSize)
		word := g.wordList[idx]
		if g.capitalize {
			word = strings.Title(word)
		} else if g.uppercase {
			word = strings.ToUpper(word)
		}
		selected[i] = word
	}
	password := strings.Join(selected, g.separator)
	if g.addDigits {
		d1 := g.randomInt(10)
		d2 := g.randomInt(10)
		password += fmt.Sprintf("%d%d", d1, d2)
	}
	if g.addSymbols {
		symbols := "!@#$%^&*()_+-="
		s1 := symbols[g.randomInt(len(symbols))]
		s2 := symbols[g.randomInt(len(symbols))]
		password += string(s1) + string(s2)
	}
	entropy := g.calculateEntropy()
	return password, entropy
}

func (g *Generator) calculateEntropy() float64 {
	dictSize := len(g.wordList)
	bits := math.Log2(float64(dictSize)) * float64(g.wordCount)
	if g.addDigits {
		bits += math.Log2(10) * 2
	}
	if g.addSymbols {
		bits += math.Log2(float64(len("!@#$%^&*()_+-="))) * 2
	}
	return bits
}

func main() {
	var (
		words      int
		separator  string
		digits     bool
		symbols    bool
		capitalize bool
		uppercase  bool
		dictFile   string
		outputFile string
		jsonOut    bool
		entropy    bool
	)
	flag.IntVar(&words, "words", 6, "Number of words")
	flag.StringVar(&separator, "separator", " ", "Word separator")
	flag.BoolVar(&digits, "digits", false, "Append 2 digits")
	flag.BoolVar(&symbols, "symbols", false, "Append 2 symbols")
	flag.BoolVar(&capitalize, "capitalize", false, "Capitalize each word")
	flag.BoolVar(&uppercase, "uppercase", false, "Uppercase all words")
	flag.StringVar(&dictFile, "dictionary", "", "Custom dictionary file")
	flag.StringVar(&outputFile, "output", "", "Save password to file")
	flag.BoolVar(&jsonOut, "json", false, "Output as JSON")
	flag.BoolVar(&entropy, "entropy", false, "Show entropy")
	flag.Parse()

	gen := NewGenerator(words, separator, digits, symbols, capitalize, uppercase, dictFile)
	password, ent := gen.Generate()

	if jsonOut {
		out := map[string]interface{}{"password": password}
		if entropy {
			out["entropy"] = ent
		}
		b, _ := json.MarshalIndent(out, "", "  ")
		fmt.Println(string(b))
	} else {
		fmt.Printf("Password: \"%s\"\n", password)
		if entropy {
			fmt.Printf("Entropy: %.2f bits\n", ent)
		}
	}

	if outputFile != "" {
		err := os.WriteFile(outputFile, []byte(password), 0644)
		if err != nil {
			fmt.Fprintf(os.Stderr, "Error saving to file: %v\n", err)
		} else {
			fmt.Printf("Password saved to %s\n", outputFile)
		}
	}
}
