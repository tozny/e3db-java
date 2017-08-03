package com.tozny.e3db;

import java.util.List;

public interface QueryResponse {
  List<Record> records();
  long last();
}
