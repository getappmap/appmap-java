package com.appland.appmap.util;

import javassist.CtBehavior;
import java.lang.reflect.Modifier;
import org.apache.commons.lang3.StringUtils;
import com.appland.appmap.output.v1.Event;

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

  /**
   * Returns canonical name of method from class name, static
   * parameter, and method name.
   *
   * @param className the class name to be checked
   * @param isStatic {@code true} if the method is static
   * @param methodName the method name to be checked
   * @return the canonical name of the method
   * 
   */
  public static String canonicalName(String className, boolean isStatic, String methodName){
    return className + (isStatic ? "." : "#") + methodName;
  }
  
  /**
   * Returns canonical name of method referenced in the event.
   * @param event the event to reference
   * @return the canonical name of the method
   */
  public static String canonicalName(Event event) {
    return canonicalName(event.definedClass, event.isStatic, event.methodId);
  }
  
  /**
   *  Returns canonical name of method described by behavior.
   * @param behavior the behavior described
   * @return the canonical name of the method
   */
  public static String canonicalName(CtBehavior behavior) {
    return canonicalName(behavior.getDeclaringClass().getName(),
                         Modifier.isStatic(behavior.getModifiers()),
                         behavior.getMethodInfo().getName());
  }
}
                                           
