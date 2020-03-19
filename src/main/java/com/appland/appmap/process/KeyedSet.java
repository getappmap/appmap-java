package com.appland.appmap.process;

import java.util.HashMap;
import java.util.HashSet;

public class KeyedSet<K, V> {
  private final HashMap<K, HashSet<V>> data = new HashMap<K, HashSet<V>>();

  public KeyedSet() {}

  public void clear() {
    data.clear();
  }

  public Boolean contains(K key, V val) {
    final HashSet<V> set = data.get(key);
    if (set == null) {
      return false;
    }

    return set.contains(val);
  }

  public Boolean add(K key, V val) {
    HashSet<V> set = data.get(key);
    if (set == null) {
      set = new HashSet<V>();
      this.data.put(key, set);
    }

    return set.add(val);
  }

  public Boolean remove(K key, V val) {
    HashSet<V> set = data.get(key);
    if (set == null) {
      return false;
    }

    return set.remove(val);
  }
}
