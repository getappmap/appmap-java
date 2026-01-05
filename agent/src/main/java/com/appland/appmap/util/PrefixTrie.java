package com.appland.appmap.util;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple Trie (Prefix Tree) for efficient prefix-based string matching.
 * This is used to check if a class name matches any of the exclusion patterns.
 */
public class PrefixTrie {
    private static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        boolean isEndOfWord = false;
    }

    private final TrieNode root;

    public PrefixTrie() {
        root = new TrieNode();
    }

    /**
     * Inserts a word into the Trie.
     * @param word The word to insert.
     */
    public void insert(String word) {
        TrieNode current = root;
        for (char ch : word.toCharArray()) {
            current = current.children.computeIfAbsent(ch, c -> new TrieNode());
        }
        current.isEndOfWord = true;
    }

    /**
     * Checks if any prefix of the given word exists in the Trie.
     * For example, if "java." is in the Trie, this will return true for "java.lang.String".
     * @param word The word to check.
     * @return {@code true} if a prefix of the word is found in the Trie, {@code false} otherwise.
     */
    public boolean startsWith(String word) {
        TrieNode current = root;
        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);
            current = current.children.get(ch);
            if (current == null) {
                return false; // No prefix match
            }
            if (current.isEndOfWord) {
                // We've found a stored pattern that is a prefix of the word.
                // e.g., Trie has "java." and word is "java.lang.String"
                return true;
            }
        }
        // The word itself is a prefix or an exact match for a pattern in the Trie
        // e.g., Trie has "java.lang" and word is "java.lang"
        return current.isEndOfWord;
    }
}
