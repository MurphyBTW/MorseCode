package com.example.morseconnect;

import java.util.*;

public class MorseDatabase {

    //  NEW STRUCTURED DATA (for LearnActivity with categories)
    public static Map<String, Map<String, String>> getCategorizedMorse() {

        Map<String, Map<String, String>> data = new LinkedHashMap<>();

        //  LETTERS
        Map<String, String> letters = new LinkedHashMap<>();
        letters.put("A", ".-");
        letters.put("B", "-...");
        letters.put("C", "-.-.");
        letters.put("D", "-..");
        letters.put("E", ".");
        letters.put("F", "..-.");
        letters.put("G", "--.");
        letters.put("H", "....");
        letters.put("I", "..");
        letters.put("J", ".---");
        letters.put("K", "-.-");
        letters.put("L", ".-..");
        letters.put("M", "--");
        letters.put("N", "-.");
        letters.put("O", "---");
        letters.put("P", ".--.");
        letters.put("Q", "--.-");
        letters.put("R", ".-.");
        letters.put("S", "...");
        letters.put("T", "-");
        letters.put("U", "..-");
        letters.put("V", "...-");
        letters.put("W", ".--");
        letters.put("X", "-..-");
        letters.put("Y", "-.--");
        letters.put("Z", "--..");

        // NUMBERS
        Map<String, String> numbers = new LinkedHashMap<>();
        numbers.put("0", "-----");
        numbers.put("1", ".----");
        numbers.put("2", "..---");
        numbers.put("3", "...--");
        numbers.put("4", "....-");
        numbers.put("5", ".....");
        numbers.put("6", "-....");
        numbers.put("7", "--...");
        numbers.put("8", "---..");
        numbers.put("9", "----.");

        //  SYMBOLS
        Map<String, String> symbols = new LinkedHashMap<>();
        symbols.put(".", ".-.-.-");
        symbols.put(",", "--..--");
        symbols.put("?", "..--..");
        symbols.put("!", "-.-.--");
        symbols.put("/", "-..-.");

        //  PROSIGNS
        Map<String, String> prosigns = new LinkedHashMap<>();
        prosigns.put("SOS", "...---...");
        prosigns.put("AR (End)", ".-.-.");
        prosigns.put("SK (Stop)", "...-.-");
        prosigns.put("BT (Break)", "-...-");

        //  WORDS
        Map<String, String> words = new LinkedHashMap<>();
        words.put("OK", "---.-");
        words.put("HI", ".... ..");
        words.put("YES", "-.-- . ...");
        words.put("NO", "-. ---");

        data.put("Letters", letters);
        data.put("Numbers", numbers);
        data.put("Symbols", symbols);
        data.put("Prosigns", prosigns);
        data.put("Words", words);

        return data;
    }

    //  OLD METHOD (FOR PRACTICE MODE — NO ERRORS)
    public static Map<String, String> getMorseMap() {
        return getCategorizedMorse().get("Letters");
    }
}