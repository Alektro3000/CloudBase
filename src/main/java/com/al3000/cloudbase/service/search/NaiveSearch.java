package com.al3000.cloudbase.service.search;

import org.springframework.stereotype.Service;

@Service
public class NaiveSearch implements StringSearchAlgorithm {

    @Override
    public String name() {
        return "Naive";
    }

    @Override
    public boolean contains(String text, String pattern) {
        if (pattern.isEmpty()) {
            return true;
        }
        if (pattern.length() > text.length()) {
            return false;
        }

        for (int offset = 0; offset <= text.length() - pattern.length(); offset++) {
            int index = 0;
            while (index < pattern.length() && text.charAt(offset + index) == pattern.charAt(index)) {
                index++;
            }
            if (index == pattern.length()) {
                return true;
            }
        }
        return false;
    }
}
