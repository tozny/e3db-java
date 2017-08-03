package com.tozny.e3db;

import java.util.Map;

public class RecordData {
  private final Map<String, String> data;
  public RecordData(Map<String, String> data) {
    if(data == null)
      throw new IllegalArgumentException("data null");

    this.data = data;
  }

  public Map<String, String> getData() {
    return data;
  }

}
