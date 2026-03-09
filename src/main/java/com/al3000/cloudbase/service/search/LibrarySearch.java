package com.al3000.cloudbase.service.search;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class LibrarySearch implements StringSearchAlgorithm {
    @Override
    public String name() {
        return "Library";
    }

    @Override
    public boolean contains(String text, String pattern) {
        return text.contains(pattern);
    }
}
