// diceware.rs
use clap::{App, Arg};
use rand::Rng;
use std::fs;
use std::io::{self, BufRead};
use std::path::Path;

// Встроенный словарь (урезанный)
const DEFAULT_WORDS: &[&str] = &[
    "a", "able", "about", "above", "abstract", "academy", "accept", "access", "account", "achieve",
    "across", "act", "action", "active", "activity", "actual", "adapt", "add", "address", "adjust",
    // ... (полный список)
    "zone", "zoo",
];

fn load_dictionary(filepath: Option<&str>) -> Vec<String> {
    if let Some(path) = filepath {
        if let Ok(file) = fs::File::open(path) {
            let reader = io::BufReader::new(file);
            let words: Vec<String> = reader.lines()
                .filter_map(Result::ok)
                .map(|s| s.trim().to_string())
                .filter(|s| !s.is_empty())
                .collect();
            if !words.is_empty() {
                return words;
            }
        }
    }
    DEFAULT_WORDS.iter().map(|s| s.to_string()).collect()
}

struct Generator {
    word_count: usize,
    separator: String,
    add_digits: bool,
    add_symbols: bool,
    capitalize: bool,
    uppercase: bool,
    word_list: Vec<String>,
}

impl Generator {
    fn new(words: usize, sep: &str, digits: bool, symbols: bool, cap: bool, up: bool, dict: Option<&str>) -> Self {
        let word_list = load_dictionary(dict);
        if word_list.len() < 7776 {
            eprintln!("Warning: word list has fewer than 7776 words, entropy may be lower.");
        }
        Generator {
            word_count: words,
            separator: sep.to_string(),
            add_digits: digits,
            add_symbols: symbols,
            capitalize: cap,
            uppercase: up,
            word_list,
        }
    }

    fn generate(&self) -> (String, f64) {
        let mut rng = rand::thread_rng();
        let mut selected = Vec::with_capacity(self.word_count);
        let dict_size = self.word_list.len();
        for _ in 0..self.word_count {
            let idx = rng.gen_range(0..dict_size);
            let mut word = self.word_list[idx].clone();
            if self.capitalize {
                if let Some(first) = word.chars().next() {
                    word = first.to_uppercase().chain(word.chars().skip(1)).collect();
                }
            } else if self.uppercase {
                word = word.to_uppercase();
            }
            selected.push(word);
        }
        let mut password = selected.join(&self.separator);
        if self.add_digits {
            let d1: u8 = rng.gen_range(0..10);
            let d2: u8 = rng.gen_range(0..10);
            password.push_str(&format!("{}{}", d1, d2));
        }
        if self.add_symbols {
            let symbols = "!@#$%^&*()_+-=";
            let s1 = symbols.chars().nth(rng.gen_range(0..symbols.len())).unwrap();
            let s2 = symbols.chars().nth(rng.gen_range(0..symbols.len())).unwrap();
            password.push(s1);
            password.push(s2);
        }
        let entropy = self.calculate_entropy();
        (password, entropy)
    }

    fn calculate_entropy(&self) -> f64 {
        let dict_size = self.word_list.len() as f64;
        let bits = dict_size.log2() * self.word_count as f64;
        let mut extra = 0.0;
        if self.add_digits { extra += (10.0_f64).log2() * 2.0; }
        if self.add_symbols { extra += ("!@#$%^&*()_+-=".len() as f64).log2() * 2.0; }
        bits + extra
    }
}

fn main() {
    let matches = App::new("Diceware Password Generator")
        .arg(Arg::with_name("words").long("words").takes_value(true).default_value("6"))
        .arg(Arg::with_name("separator").long("separator").takes_value(true).default_value(" "))
        .arg(Arg::with_name("digits").long("digits"))
        .arg(Arg::with_name("symbols").long("symbols"))
        .arg(Arg::with_name("capitalize").long("capitalize"))
        .arg(Arg::with_name("uppercase").long("uppercase"))
        .arg(Arg::with_name("dictionary").long("dictionary").takes_value(true))
        .arg(Arg::with_name("output").long("output").takes_value(true))
        .arg(Arg::with_name("json").long("json"))
        .arg(Arg::with_name("entropy").long("entropy"))
        .get_matches();

    let words: usize = matches.value_of("words").unwrap().parse().expect("Invalid number");
    let separator = matches.value_of("separator").unwrap();
    let digits = matches.is_present("digits");
    let symbols = matches.is_present("symbols");
    let capitalize = matches.is_present("capitalize");
    let uppercase = matches.is_present("uppercase");
    let dict = matches.value_of("dictionary");
    let output = matches.value_of("output");
    let json_out = matches.is_present("json");
    let show_entropy = matches.is_present("entropy");

    let gen = Generator::new(words, separator, digits, symbols, capitalize, uppercase, dict);
    let (password, entropy) = gen.generate();

    if json_out {
        let mut out = serde_json::json!({ "password": password });
        if show_entropy {
            out["entropy"] = serde_json::json!(entropy);
        }
        println!("{}", serde_json::to_string_pretty(&out).unwrap());
    } else {
        println!("Password: \"{}\"", password);
        if show_entropy {
            println!("Entropy: {:.2} bits", entropy);
        }
    }

    if let Some(path) = output {
        if let Err(e) = fs::write(path, password) {
            eprintln!("Error saving to file: {}", e);
        } else {
            println!("Password saved to {}", path);
        }
    }
}
