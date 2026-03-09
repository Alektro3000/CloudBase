package com.al3000.cloudbase.service.search;

import org.springframework.stereotype.Service;

@Service
public class KMPSearch implements StringSearchAlgorithm {

    @Override
    public String name() {
        return "Knuth-Morris-Pratt";
    }

    @Override
    public boolean contains(String text, String pattern) {
        if (pattern.isEmpty()) {
            return true;
        }
        if (pattern.length() > text.length()) {
            return false;
        }

        int[] lps = longestPrefixSuffix(pattern);
        int textIndex = 0;
        int patternIndex = 0;

        while (textIndex < text.length()) {
            if (text.charAt(textIndex) == pattern.charAt(patternIndex)) {
                textIndex++;
                patternIndex++;
                if (patternIndex == pattern.length()) {
                    return true;
                }
                continue;
            }

            if (patternIndex == 0) {
                textIndex++;
            } else {
                patternIndex = lps[patternIndex - 1];
            }
        }

        return false;
    }

    private int[] longestPrefixSuffix(String pattern) {
        int[] lps = new int[pattern.length()];
        int length = 0;
        int index = 1;

        while (index < pattern.length()) {
            if (pattern.charAt(index) == pattern.charAt(length)) {
                lps[index] = ++length;
                index++;
                continue;
            }

            if (length == 0) {
                lps[index] = 0;
                index++;
            } else {
                length = lps[length - 1];
            }
        }

        return lps;
    }
}
