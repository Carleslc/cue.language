/*
   Copyright 2009 IBM Corp

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package cue.lang;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.*;

/**
 * 
 * @author Jonathan Feinberg <jdf@us.ibm.com>, modified by Carlos Lázaro Costa
 * 
 */
public enum StopWords {
    Arabic("ar"), Armenian("hy"), Catalan("ca"), Croatian("hr"), Czech("cs"), Dutch("nl"),
    Danish("da"), English("en"), Esperanto("eo"), Farsi("fa"), Finnish("fi"),
    French("fr"), German("de"), Greek("el"), Hindi("hi"), Hungarian("hu"),
    Italian("it"), Latin("la"), Norwegian("no"), Polish("pl"), Portuguese("pt"),
    Romanian("ro"), Russian("ru"), Slovenian("sl"), Slovak("sk"), Spanish("es"),
    Swedish("sv"), Hebrew("he"), Turkish("tr"), Custom(null);

    public static String DEFAULT_DELIMITERS = " \t\n\r\f";

    private Locale locale;
    private String language;

    StopWords(String locale) {
        this.language = locale;
        if (locale != null) {
            this.locale = new Locale(locale);
        }
    }

    public String getLanguage() {
        return language;
    }

    public static StopWords guess(final String text) {
        return guess(new Counter<>(new WordIterator(text)));
    }

    public static StopWords guess(final Counter<String> wordCounter) {
        return guess(wordCounter.getMostFrequent(50));
    }

    public static StopWords guess(final Collection<String> words) {
        StopWords currentWinner = null;
        int currentMax = 0;
        for (final StopWords stopWords : StopWords.values()) {
            int count = 0;
            for (final String word : words) {
                if (stopWords.isStopWord(word)) {
                    count++;
                }
            }
            if (count > currentMax) {
                currentWinner = stopWords;
                currentMax = count;
            }
        }
        return currentWinner;
    }

    private final Set<String> stopWords = new HashSet<>();

    public boolean isStopWord(final String s) {
        return s.length() == 1 || stopWords.contains(s.toLowerCase(locale));
    }

    public boolean isStopWordExact(final String s) { return s.length() == 1 || stopWords.contains(s); }

    public String remove(final String s, String delimiters) {
        if (s == null) return null;
        if (s.isEmpty()) return "";
        if (delimiters == null || delimiters.isEmpty()) delimiters = DEFAULT_DELIMITERS;

        String trimmed = s.replaceAll("[\\W\\d]", " ");

        StringTokenizer tokenizer = new StringTokenizer(trimmed, delimiters);

        if (stopWords.isEmpty()) {
            loadLanguage();
        }

        StringBuilder builder = new StringBuilder();

        while (tokenizer.hasMoreTokens()) {
            String word = tokenizer.nextToken();
            if (!isStopWord(word)) {
                builder.append(word).append(" ");
            }
        }

        return builder.toString().trim();
    }

    public String remove(final String s) {
        return remove(s, null);
    }

    public void loadLanguage() {
        final String wordlistResource = name().toLowerCase(Locale.ENGLISH);
        readStopWords(Thread.currentThread().getContextClassLoader().getResourceAsStream(wordlistResource), Charset.forName("UTF-8"));
    }

    private void readStopWords(final InputStream inputStream, final Charset encoding) {
        try {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, encoding))) {
                String line;
                while ((line = in.readLine()) != null) {
                    line = line.replaceAll("\\|.*", "").trim();
                    if (line.length() == 0) {
                        continue;
                    }
                    for (final String w : line.split("\\s+")) {
                        stopWords.add(w.toLowerCase(locale));
                    }
                }
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
