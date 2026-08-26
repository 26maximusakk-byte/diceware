
---

# Код на 8 языках программирования

## 1. Python (`diceware.py`)

```python
# diceware.py
import argparse
import json
import math
import random
import sys
import os

# Встроенный словарь Diceware (первые 100 слов для примера; полный словарь должен быть загружен из файла)
# В реальном коде мы загружаем полный список из 7776 слов.
DICEWARE_WORDS = [
    "a", "able", "about", "above", "abstract", "academy", "accept", "access", "account", "achieve",
    "across", "act", "action", "active", "activity", "actual", "adapt", "add", "address", "adjust",
    # ... (здесь должен быть полный список, но для краткости опущен)
    "zone", "zoo"
]  # Для демонстрации используем урезанный список. В реальном проекте следует загружать полный словарь из файла.

# Для демонстрации создадим загрузку из файла, если есть
def load_dictionary(filepath=None):
    if filepath and os.path.exists(filepath):
        with open(filepath, 'r', encoding='utf-8') as f:
            return [line.strip() for line in f if line.strip()]
    # Если файл не указан или не существует, используем встроенный урезанный список
    # В реальном проекте здесь должен быть полный список из 7776 слов.
    # Для теста используем генерацию случайных слов из небольшого набора.
    # Но для полноты сгенерируем список из 7776 слов с помощью базовых слов.
    # Чтобы не раздувать код, используем предопределённый список.
    return DICEWARE_WORDS

class DicewareGenerator:
    def __init__(self, words=6, separator=' ', digits=False, symbols=False,
                 capitalize=False, uppercase=False, dictionary=None):
        self.word_count = words
        self.separator = separator
        self.add_digits = digits
        self.add_symbols = symbols
        self.capitalize = capitalize
        self.uppercase = uppercase
        self.word_list = load_dictionary(dictionary)
        if len(self.word_list) < 7776:
            print("Warning: word list has fewer than 7776 words, entropy may be lower.", file=sys.stderr)

    def generate(self):
        # Выбираем случайные слова
        selected = random.choices(self.word_list, k=self.word_count)
        # Применяем преобразования
        if self.capitalize:
            selected = [w.capitalize() for w in selected]
        elif self.uppercase:
            selected = [w.upper() for w in selected]
        password = self.separator.join(selected)
        # Добавляем цифры и символы
        if self.add_digits:
            digits = ''.join(random.choices('0123456789', k=2))
            password += digits
        if self.add_symbols:
            symbols = ''.join(random.choices('!@#$%^&*()_+-=', k=2))
            password += symbols
        # Вычисляем энтропию
        entropy = self.calculate_entropy()
        return password, entropy

    def calculate_entropy(self):
        # Энтропия = log2(количество слов в словаре) * количество слов
        # + дополнительные биты от цифр и символов
        dict_size = len(self.word_list)
        bits = math.log2(dict_size) * self.word_count
        if self.add_digits:
            bits += math.log2(10) * 2  # 2 цифры
        if self.add_symbols:
            bits += math.log2(len('!@#$%^&*()_+-=')) * 2
        return bits

def main():
    parser = argparse.ArgumentParser(description="Diceware password generator")
    parser.add_argument("--words", type=int, default=6, help="Number of words (default: 6)")
    parser.add_argument("--separator", default=" ", help="Word separator (default: space)")
    parser.add_argument("--digits", action="store_true", help="Append 2 digits")
    parser.add_argument("--symbols", action="store_true", help="Append 2 symbols")
    parser.add_argument("--capitalize", action="store_true", help="Capitalize each word")
    parser.add_argument("--uppercase", action="store_true", help="Uppercase all words")
    parser.add_argument("--dictionary", help="Custom dictionary file path")
    parser.add_argument("--output", help="Save password to file")
    parser.add_argument("--json", action="store_true", help="Output as JSON")
    parser.add_argument("--entropy", action="store_true", help="Show entropy")
    args = parser.parse_args()

    gen = DicewareGenerator(
        words=args.words,
        separator=args.separator,
        digits=args.digits,
        symbols=args.symbols,
        capitalize=args.capitalize,
        uppercase=args.uppercase,
        dictionary=args.dictionary
    )
    password, entropy = gen.generate()

    if args.json:
        output = {"password": password, "entropy": entropy if args.entropy else None}
        print(json.dumps(output, indent=2))
    else:
        print(f"Password: \"{password}\"")
        if args.entropy:
            print(f"Entropy: {entropy:.2f} bits")

    if args.output:
        with open(args.output, 'w') as f:
            f.write(password)
        print(f"Password saved to {args.output}")

if __name__ == "__main__":
    main()
