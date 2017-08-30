package com.tozny.e3db;

import java.util.List;
import java.util.Map;

public interface Record {
  RecordMeta meta();
  Map<String, String> data();
}
