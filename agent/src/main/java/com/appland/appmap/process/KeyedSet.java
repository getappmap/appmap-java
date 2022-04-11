package com.appland.appmap.process;

import java.util.HashMap;
import java.util.HashSet;

/**
 * A KeyedSet provides access to any number of HashSets by some key.
 * @param <K> The key type
 * @param <V> The set value type
 */
public class KeyedSet<K, V> {
  private final HashMap<K, HashSet<V>> data = new HashMap<K, HashSet<V>>();

  public KeyedSet() {}

  public void clear() {
    data.clear();
  }

  /**
   * Check if the set at the given key contains a particular value.
   * @param key The set key
   * @param val The contained value
   * @return {@code true} if the set contained the value. Otherwise, {@code false}.
   */
  public Boolean contains(K key, V val) {
    final HashSet<V> set = data.get(key);
    if (set == null) {
      return false;
    }

    return set.contains(val);
  }

  /**
   * Add a value to the set at a given key.
   * @param key The set key
   * @param val The value to be added
   * @return {@code false} if the set already contained the value. Otherwise, {@code true}.
   */
  public Boolean add(K key, V val) {
    HashSet<V> set = data.get(key);
    if (set == null) {
      set = new HashSet<V>();
      this.data.put(key, set);
    }

    return set.add(val);
  }

  /**
   * Remove a value from a set at a given key.
   * @param key The set key
   * @param val The value to be removed
   * @return {@code true} if the value was removed from the set. Otherwise, the set did not contain
   *         the value and {@code false} is returned.
   */
  public Boolean remove(K key, V val) {
    HashSet<V> set = data.get(key);
    if (set == null) {
      return false;
    }

    return set.remove(val);
  }
}
