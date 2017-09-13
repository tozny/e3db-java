package com.tozny.e3db;

import java.util.Map;

/**
 * Static methods to check for null, empty string, etc.
 */
public class Checks {
  /**
   * Throws if the given string is null or blank.
   */
  public static void checkNotEmpty(String str, String name) {
    if(str == null || str.trim().length() == 0)
      throw new IllegalArgumentException(name + ": (string): was null or empty.");
  }

  /**
   * Throws if the given array is null or empty.
   */
  public static void checkNotEmpty(byte[] arr, String name) {
    if(arr == null || arr.length == 0)
      throw new IllegalArgumentException(name + ": (array): was null or empty");
  }

  /**
   * Throws if the given value is null.
   */
  public static <R> void checkNotNull(R obj, String name) {
    if(obj == null)
      throw new IllegalArgumentException(name + ": (object): was null.");
  }

  /**
   * Throws if the given map is empty, if it contains a key that is empty,
   * or if it contains a value that is null.
   */
  public static void checkMap(Map<String, String> map, String name) {
    if(map.size() == 0)
      throw new IllegalArgumentException(name + ": (map): empty.");

    for(Map.Entry<String, String> entries : map.entrySet()) {
      checkNotEmpty(entries.getKey(), name + ": (map): field name");
      checkNotNull(entries.getValue(), name + ": (map): field value");
    }
  }
}
