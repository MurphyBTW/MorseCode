
package com.example.morseconnect;

import java.util.*;

public class MorseDatabase {

    public static Map<String, Map<String, String>> getCategorizedMorse() {

        Map<String, Map<String, String>> data = new LinkedHashMap<>();

        // LETTERS
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

        // SYMBOLS
        Map<String, String> symbols = new LinkedHashMap<>();
        symbols.put(".", ".-.-.-");
        symbols.put(",", "--..--");
        symbols.put("?", "..--..");
        symbols.put("!", "-.-.--");
        symbols.put("/", "-..-.");

        // PROSIGNS AND COMMON PROCEDURAL SIGNALS
        // Prosigns are sent as one continuous Morse sequence.
        // Common operating abbreviations are separated by spaces.
        Map<String, String> prosigns = new LinkedHashMap<>();

        prosigns.put("AR (End of message)", ".-.-.");
        prosigns.put("SK (End of contact)", "...-.-");
        prosigns.put("BT (Separator)", "-...-");
        prosigns.put("AS (Wait)", ".-...");
        prosigns.put("KA (Start signal)", "-.-.-");
        prosigns.put("HH (Error)", "........");
        prosigns.put("KN (Over to named station)", "-.--.");
        prosigns.put("BK (Break-in)", "-...-.-");
        prosigns.put("CL (Closing station)", "-.-..-..");
        prosigns.put("CT (Starting signal)", "-.-.-");
        prosigns.put("SN (Understood)", "...-.");
        prosigns.put("VA (End of work)", "...-.-");
        prosigns.put("SOS (Distress)", "...---...");
        prosigns.put("IMI (Repeat)", "..--..");
        prosigns.put("INT (Interrogative)", "..-.-");

        // Common operating abbreviations (letters sent separately)
        prosigns.put("CQ (Calling any station)", "-.-. --.-");
        prosigns.put("DE (This is)", "-.. .");
        prosigns.put("K (Go ahead)", "-.-");
        prosigns.put("R (Received)", ".-.");
        prosigns.put("QSL (Confirmation)", "--.- ... .-..");

        // WORDS
        // Each letter is separated by a space for readable playback.
        Map<String, String> words = new LinkedHashMap<>();

        words.put("HI", ".... ..");
        words.put("NO", "-. ---");
        words.put("YES", "-.-- . ...");
        words.put("GO", "--. ---");
        words.put("STOP", "... - --- .--.");
        words.put("HELP", ".... . .-.. .--.");
        words.put("LOVE", ".-.. --- ...- .");
        words.put("CODE", "-.-. --- -.. .");
        words.put("TEST", "- . ... -");
        words.put("RUN", ".-. ..- -.");
        words.put("WAIT", ".-- .- .. -");
        words.put("COME", "-.-. --- -- .");
        words.put("HERE", ".... . .-. .");
        words.put("SAFE", "... .- ..-. .");
        words.put("HOME", ".... --- -- .");
        words.put("OK", "--- -.-");
        words.put("BYE", "-... -.-- .");
        words.put("CAT", "-.-. .- -");
        words.put("DOG", "-.. --- --.");
        words.put("UP", "..- .--.");

        data.put("Letters", letters);
        data.put("Numbers", numbers);
        data.put("Symbols", symbols);
        data.put("Prosigns", prosigns);
        data.put("Words", words);

        return data;
    }

    // OLD METHOD FOR PRACTICE MODE
    public static Map<String, String> getMorseMap() {
        return getCategorizedMorse().get("Letters");
    }
}