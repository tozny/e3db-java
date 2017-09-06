package com.tozny.e3db;

import java.util.Map;
import static com.tozny.e3db.Checks.*;

public class RecordData {
  private final Map<String, String> cleartext;

  public RecordData(Map<String, String> cleartext) {
    if(cleartext == null)
      throw new IllegalArgumentException("cleartext null");

    checkMap(cleartext, "cleartext");
    this.cleartext = cleartext;
  }

  public Map<String, String> getCleartext() {
    return cleartext;
  }

}
