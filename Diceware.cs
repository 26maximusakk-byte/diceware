// Diceware.cs
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Security.Cryptography;
using System.Text;
using Newtonsoft.Json;

namespace Diceware
{
    class Program
    {
        private static readonly string[] DefaultWords = {
            "a", "able", "about", "above", "abstract", "academy", "accept", "access", "account", "achieve",
            "across", "act", "action", "active", "activity", "actual", "adapt", "add", "address", "adjust",
            // ... (полный список)
            "zone", "zoo"
        };

        private List<string> wordList;
        private int wordCount;
        private string separator;
        private bool addDigits;
        private bool addSymbols;
        private bool capitalize;
        private bool uppercase;
        private RandomNumberGenerator rng = RandomNumberGenerator.Create();

        public Diceware(int words, string sep, bool digits, bool symbols,
                        bool cap, bool up, string dictFile)
        {
            wordCount = words;
            separator = sep;
            addDigits = digits;
            addSymbols = symbols;
            capitalize = cap;
            uppercase = up;
            wordList = LoadDictionary(dictFile);
            if (wordList.Count < 7776)
                Console.Error.WriteLine("Warning: word list has fewer than 7776 words, entropy may be lower.");
        }

        private List<string> LoadDictionary(string filepath)
        {
            if (!string.IsNullOrEmpty(filepath) && File.Exists(filepath))
            {
                var lines = File.ReadAllLines(filepath)
                                .Select(s => s.Trim())
                                .Where(s => !string.IsNullOrEmpty(s))
                                .ToList();
                if (lines.Count > 0) return lines;
            }
            return new List<string>(DefaultWords);
        }

        private int RandomInt(int max)
        {
            byte[] bytes = new byte[4];
            rng.GetBytes(bytes);
            uint val = BitConverter.ToUInt32(bytes, 0);
            return (int)(val % max);
        }

        public (string password, double entropy) Generate()
        {
            var selected = new List<string>();
            int dictSize = wordList.Count;
            for (int i = 0; i < wordCount; i++)
            {
                string word = wordList[RandomInt(dictSize)];
                if (capitalize)
                    word = char.ToUpper(word[0]) + word.Substring(1);
                else if (uppercase)
                    word = word.ToUpper();
                selected.Add(word);
            }
            string password = string.Join(separator, selected);
            if (addDigits)
            {
                int d1 = RandomInt(10);
                int d2 = RandomInt(10);
                password += d1.ToString() + d2.ToString();
            }
            if (addSymbols)
            {
                string symbols = "!@#$%^&*()_+-=";
                char s1 = symbols[RandomInt(symbols.Length)];
                char s2 = symbols[RandomInt(symbols.Length)];
                password += s1.ToString() + s2.ToString();
            }
            double entropy = CalculateEntropy();
            return (password, entropy);
        }

        private double CalculateEntropy()
        {
            int dictSize = wordList.Count;
            double bits = Math.Log(dictSize, 2) * wordCount;
            if (addDigits) bits += Math.Log(10, 2) * 2;
            if (addSymbols) bits += Math.Log("!@#$%^&*()_+-=".Length, 2) * 2;
            return bits;
        }

        static void Main(string[] args)
        {
            int words = 6;
            string separator = " ";
            bool digits = false, symbols = false, capitalize = false, uppercase = false;
            string dictFile = null, outputFile = null;
            bool jsonOut = false, entropy = false;

            for (int i = 0; i < args.Length; i++)
            {
                switch (args[i])
                {
                    case "--words": words = int.Parse(args[++i]); break;
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

            var gen = new Diceware(words, separator, digits, symbols, capitalize, uppercase, dictFile);
            var result = gen.Generate();
            string password = result.password;
            double ent = result.entropy;

            if (jsonOut)
            {
                var obj = new { password = password, entropy = entropy ? (double?)ent : null };
                Console.WriteLine(JsonConvert.SerializeObject(obj, Formatting.Indented));
            }
            else
            {
                Console.WriteLine($"Password: \"{password}\"");
                if (entropy)
                    Console.WriteLine($"Entropy: {ent:F2} bits");
            }

            if (!string.IsNullOrEmpty(outputFile))
            {
                File.WriteAllText(outputFile, password);
                Console.WriteLine($"Password saved to {outputFile}");
            }
        }
    }
}
