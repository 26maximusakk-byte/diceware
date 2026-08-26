#!/usr/bin/env node
// diceware.js
const { program } = require('commander');
const fs = require('fs');
const crypto = require('crypto');

// Встроенный словарь (урезанный для примера)
const DEFAULT_WORDS = ["a", "able", "about", "above", "abstract", "academy", "accept", "access", "account", "achieve",
    "across", "act", "action", "active", "activity", "actual", "adapt", "add", "address", "adjust",
    // ... (полный список должен быть загружен из файла)
    "zone", "zoo"];

function loadDictionary(filepath) {
    if (filepath && fs.existsSync(filepath)) {
        const content = fs.readFileSync(filepath, 'utf8');
        return content.split('\n').map(s => s.trim()).filter(Boolean);
    }
    return DEFAULT_WORDS;
}

class DicewareGenerator {
    constructor(options) {
        this.wordCount = options.words || 6;
        this.separator = options.separator || ' ';
        this.addDigits = options.digits || false;
        this.addSymbols = options.symbols || false;
        this.capitalize = options.capitalize || false;
        this.uppercase = options.uppercase || false;
        this.wordList = loadDictionary(options.dictionary);
        if (this.wordList.length < 7776) {
            console.warn('Warning: word list has fewer than 7776 words, entropy may be lower.');
        }
    }

    generate() {
        // Выбираем случайные слова с использованием crypto для безопасности
        const selected = [];
        const len = this.wordList.length;
        for (let i = 0; i < this.wordCount; i++) {
            const idx = crypto.randomInt(0, len);
            selected.push(this.wordList[idx]);
        }
        // Преобразования
        let processed = selected.map(w => {
            if (this.capitalize) return w.charAt(0).toUpperCase() + w.slice(1);
            if (this.uppercase) return w.toUpperCase();
            return w;
        });
        let password = processed.join(this.separator);
        if (this.addDigits) {
            const digits = Array.from({length: 2}, () => crypto.randomInt(0, 10)).join('');
            password += digits;
        }
        if (this.addSymbols) {
            const symbols = '!@#$%^&*()_+-=';
            const sym = Array.from({length: 2}, () => symbols[crypto.randomInt(0, symbols.length)]).join('');
            password += sym;
        }
        const entropy = this.calculateEntropy();
        return { password, entropy };
    }

    calculateEntropy() {
        const dictSize = this.wordList.length;
        let bits = Math.log2(dictSize) * this.wordCount;
        if (this.addDigits) bits += Math.log2(10) * 2;
        if (this.addSymbols) bits += Math.log2('!@#$%^&*()_+-='.length) * 2;
        return bits;
    }
}

program
    .option('-w, --words <number>', 'Number of words', parseInt, 6)
    .option('-s, --separator <char>', 'Word separator', ' ')
    .option('-d, --digits', 'Append 2 digits')
    .option('--symbols', 'Append 2 symbols')
    .option('--capitalize', 'Capitalize each word')
    .option('--uppercase', 'Uppercase all words')
    .option('--dictionary <file>', 'Custom dictionary file')
    .option('--output <file>', 'Save password to file')
    .option('--json', 'Output as JSON')
    .option('--entropy', 'Show entropy')
    .parse(process.argv);

const opts = program.opts();
const gen = new DicewareGenerator(opts);
const result = gen.generate();

if (opts.json) {
    const out = { password: result.password };
    if (opts.entropy) out.entropy = result.entropy;
    console.log(JSON.stringify(out, null, 2));
} else {
    console.log(`Password: "${result.password}"`);
    if (opts.entropy) {
        console.log(`Entropy: ${result.entropy.toFixed(2)} bits`);
    }
}

if (opts.output) {
    fs.writeFileSync(opts.output, result.password);
    console.log(`Password saved to ${opts.output}`);
}
