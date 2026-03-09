package com.al3000.cloudbase.service.search;

public interface StringSearchAlgorithm {

    String name();

    boolean contains(String text, String pattern);
}
