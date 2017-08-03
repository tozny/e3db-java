package com.tozny.e3db;

import java.util.List;
import java.util.Map;

public interface Record {
  RecordMeta meta();
  List<String> keys();
  List<Object> values();
  Map<String, String> entries();
  String get(String key);
  boolean containsKey(String key);
}
