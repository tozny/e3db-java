package com.tozny.e3db;

import java.util.Map;

public class Checks {
  public static void checkNotEmpty(String str, String name) {
    if(str == null || str.trim().length() == 0)
      throw new IllegalArgumentException(name + ": (string): was null or empty.");
  }

  public static void checkNotEmpty(byte[] arr, String name) {
    if(arr == null || arr.length == 0)
      throw new IllegalArgumentException(name + ": (array): was null or empty");
  }

  public static <R> void checkNotNull(R obj, String name) {
    if(obj == null)
      throw new IllegalArgumentException(name + ": (object): was null.");
  }

  public static void checkMap(Map<String, String> map, String name) {
    if(map.size() == 0)
      throw new IllegalArgumentException(name + ": (map): empty.");

    for(Map.Entry<String, String> entries : map.entrySet()) {
      checkNotEmpty(entries.getKey(), name + ": (map): field name");
      checkNotNull(entries.getValue(), name + ": (map): field value");
    }
  }
}
