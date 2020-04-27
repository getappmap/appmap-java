package com.appland.appmap.util;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility methods to format strings.
 */
public class StringUtil {
  /**
   * Returns a copy of a string with the first character capitalized.
   */
  public static String capitalize(String str) {
    return new String(str.substring(0, 1).toUpperCase() + str.substring(1));
  }

  /**
   * Returns a copy of a string with the first character decapitalized.
   */
  public static String decapitalize(String str) {
    return new String(str.substring(0, 1).toLowerCase() + str.substring(1));
  }

  /**
   * Returns whether or not a string appears to be an acronym.
   */
  public static Boolean isAcronym(String str) {
    return !decapitalize(str).equals(str.toLowerCase());
  }

  /**
   * Formats an identifier as a sentence.
   * Ex: `com.myorg.MyClass` -> `My class`
   */
  public static String identifierToSentence(String identifier) {
    String[] packages = StringUtils.split(identifier, '.');
    String shortPackage = packages[packages.length - 1];
    shortPackage = StringUtils.replace(shortPackage, "Test", "");
    String[] words = StringUtils.splitByCharacterTypeCamelCase(shortPackage);
    String[] formattedWords = new String[words.length];
    for (int i = 0; i < formattedWords.length; i++) {
      String word = words[i];
      if (word.length() == 0) {
        continue;
      }

      if (isAcronym(word)) {
        formattedWords[i] = word;
      } else {
        formattedWords[i] = word.toLowerCase();
      }
    }

    return capitalize(StringUtils.join(formattedWords, ' '));
  }
}
